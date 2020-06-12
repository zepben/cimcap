// Copyright 2019 Zeppelin Bend Pty Ltd
// This file is part of cimcap.
//
// cimcap is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// cimcap is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with cimcap.  If not, see <https://www.gnu.org/licenses/>.


package com.zepben.cimcap

import ch.qos.logback.classic.Level
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.zepben.auth.JWTAuthenticator
import com.zepben.auth.grpc.AuthInterceptor
import com.zepben.cimbend.database.sqlite.DatabaseWriter
import com.zepben.cimcap.auth.ConfigServer
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder
import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Statement
import kotlin.system.exitProcess

const val write_network_scope = "write:ewb"
val requiredScopes = mapOf(
    "zepben.protobuf.np.NetworkProducer" to write_network_scope,
    "zepben.protobuf.dp.DiagramProducer" to write_network_scope,
    "zepben.protobuf.cp.CustomerProducer" to write_network_scope
)

/**
 * @property domain The domain on which to fetch tokens from. Must expose its JWKS on <domain>/.well_known/jwks.json
 */
class CIMDBServer(
    val port: Int = 50051,
    certChainFilePath: String? = null,
    privateKeyFilePath: String? = null,
    trustCertCollectionFilePath: String? = null,
    clientAuth: ClientAuth = ClientAuth.OPTIONAL,
    audience: String? = null,
    domain: String? = null,
    private val databaseFile: String = "cim.db",
    private val getConnection: (String) -> Connection = DriverManager::getConnection,
    private val getStatement: (Connection) -> Statement = Connection::createStatement,
    private val getPreparedStatement: (Connection, String) -> PreparedStatement = Connection::prepareStatement,
    private var networkServicer: NetworkProducerServer = NetworkProducerServer(),
    private var diagramServicer: DiagramProducerServer = DiagramProducerServer(),
    private var customerServicer: CustomerProducerServer = CustomerProducerServer()
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private var networkSent = false
    private var diagramSent = false
    private var customerSent = false
    private val serverBuilder = NettyServerBuilder.forPort(port)
        .addService(networkServicer)
        .addService(diagramServicer)
        .addService(customerServicer)

    init {
        createSslContext(certChainFilePath, privateKeyFilePath, trustCertCollectionFilePath, clientAuth)?.let {
            serverBuilder.sslContext(it)
        }
        audience?.let { aud ->
            domain?.let { dom ->
                serverBuilder.intercept(
                    AuthInterceptor(
                        JWTAuthenticator(aud, dom),
                        requiredScopes
                    )
                )
            }
        }
        networkServicer.addCallback { networkSent = true; handleComplete(it) }
        diagramServicer.addCallback { diagramSent = true; handleComplete(it) }
        customerServicer.addCallback { customerSent = true; handleComplete(it) }
    }

    private val server = serverBuilder.build()

    private val isComplete
        get() = networkSent && diagramSent && customerSent

    private fun handleComplete(errors: Sequence<String>) {
        errors.forEach {
            logger.error(it)
        }
        if (isComplete) {
            val writer = DatabaseWriter(databaseFile, getConnection, getStatement, getPreparedStatement)
            if (writer.save(listOf(networkServicer.networkService, diagramServicer.diagramService, customerServicer.customerService))) {
                logger.info("Database saved to $databaseFile")
                networkServicer.resetNetworkService()
                diagramServicer.resetDiagramService()
                customerServicer.resetCustomerService()
                networkSent = false
                diagramSent = false
                customerSent = false
                return
            } else {
                logger.error("Save could not be completed")
                throw Exception("Database failed to save")
            }
        }
    }

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                this@CIMDBServer.stop()
                logger.info("Stopped CimDbServer")
            }
        )
    }

    fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }
}

/**
 * Create an SSLContext for use with the gRPC server.
 * @return null if a private key or cert chain are not provided, otherwise an SSLContext with the provided
 * credentials.
 */
fun createSslContext(
    certChainFilePath: String? = null,
    privateKeyFilePath: String? = null,
    trustCertCollectionFilePath: String? = null,
    clientAuth: ClientAuth = ClientAuth.OPTIONAL
): SslContext? {
    if (privateKeyFilePath.isNullOrBlank() || certChainFilePath.isNullOrBlank())
        return null

    val sslClientContextBuilder = SslContextBuilder.forServer(
        File(certChainFilePath),
        File(privateKeyFilePath)
    )
    if (!trustCertCollectionFilePath.isNullOrBlank()) {
        sslClientContextBuilder.trustManager(File(trustCertCollectionFilePath))
        sslClientContextBuilder.clientAuth(clientAuth)
    }
    return GrpcSslContexts.configure(sslClientContextBuilder).build()
}

class Args(parser: ArgParser) {
    val port by parser.storing("-p", "--port", help = "Port for gRPC server") { toInt() }.default(50051)
    val confPort by parser.storing("--conf-port", help = "Port for HTTP auth config server") { toInt() }.default(8080)
    val privateKeyFilePath by parser.storing("-k", "--key", help = "Private key").default(null)
    val certChainFilePath by parser.storing("-c", "--cert", help = "Certificate chain for private key").default(null)
    val trustCertCollectionFilePath by parser.storing("-a", "--cacert", help = "CA Certificate chain").default(null)
    val tokenAuth by parser.flagging("-t", "--token-auth", help = "Token authentication (Auth0 M2M).").default(false)
    val clientAuth by parser.flagging("--client-auth", help = "Require client authentication.").default(false)
    val dbFile by parser.storing("-d", "--db", help = "Database output file location").default("cim.db")
    val audience by parser.storing("--audience", help = "Auth0 Audience for this application")
        .default("https://evolve-ingestor/")
    val domain by parser.storing("--domain", help = "Auth0 domain to use").default("zepben.au.auth0.com")
    val tokenLookup by parser.storing("--token-url", help = "Token fetch URL to use").default("https://zepben.au.auth0.com/oauth/token")
    val algorithm by parser.storing("--alg", help = "Auth0 Algorithm to use").default("RS256")

}

fun main(args: Array<String>) {
    // This is to stop the gRPC lib spamming debug messages. Need to figure out how to stop it in a cleaner way.
    val logger: Logger = LoggerFactory.getLogger("main")
    val root = LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
    root.level = Level.INFO
    val ret = try {
        runBlocking {
            val vertx = Vertx.vertx()
            ArgParser(args).parseInto(::Args).run {
                val server = try {
                    CIMDBServer(
                        port,
                        certChainFilePath,
                        privateKeyFilePath,
                        trustCertCollectionFilePath,
                        if (clientAuth) ClientAuth.REQUIRE else ClientAuth.OPTIONAL,
                        if (tokenAuth) audience else null,
                        if (tokenAuth) domain else null,
                        dbFile
                    )
                } catch (e: IllegalArgumentException) {
                    logger.error("Failed to create CIMDBServer. Error was: ${e.message}")
                    logger.debug("", e)
                    return@runBlocking -1
                }
                logger.info("Starting CIMDBServer")
                server.start()
                logger.info("CIMDBServer running on 0.0.0.0:${port}")

                val confServer = ConfigServer(vertx, confPort, audience, tokenLookup, algorithm)
                logger.info("Starting AuthConfig server")
                try {
                    confServer.start()

                    Runtime.getRuntime().addShutdownHook(Thread {
                        runBlocking {
                            confServer.close()
                            vertx.close()
                        }
                    })
                } catch (e: Exception) {
                    logger.error("Failed to start AuthConfig server: ${e.message}")
                    logger.debug("", e)
                    return@runBlocking -2
                }
                logger.info("AuthConfig HTTP server running on 0.0.0.0:${confPort}")

                server.blockUntilShutdown()
            }
            0
        }
    } catch (e: Exception) {
        logger.error("Failed to launch server.", e)
        -3
    }
    logger.info("Shutdown commenced")
    exitProcess(ret)
}
