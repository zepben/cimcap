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

import com.zepben.cimbend.diagram.DiagramProtoToCim
import com.zepben.cimbend.diagram.DiagramService
import com.zepben.protobuf.dp.*
import io.grpc.Status
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DiagramProducerServer(onComplete: List<(Sequence<String>) -> Unit>? = null) : DiagramProducerGrpcKt.DiagramProducerCoroutineImplBase(), CallsBack{

    private val callbacks = mutableListOf<(Sequence<String>) -> Unit>()
    var diagramService = DiagramService()
        private set
    private var diagramToCim = DiagramProtoToCim(diagramService)
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val lock = ReentrantLock()

    init {
        onComplete?.forEach { callbacks.add(it) }
    }

    fun resetDiagramService() {
        diagramService = DiagramService()
        diagramToCim = DiagramProtoToCim(diagramService)
    }

    override fun addCallback(callback: (Sequence<String>) -> Unit) {
        callbacks.add(callback)
    }

    override suspend fun createDiagramService(request: CreateDiagramServiceRequest): CreateDiagramServiceRequest {
        lock.withLock {
            resetDiagramService()
        }
        return CreateDiagramServiceRequest.getDefaultInstance()
    }

    override suspend fun completeDiagramService(request: CompleteDiagramServiceRequest): CompleteDiagramServiceRequest {
        val errors = diagramService.validateReferences()
        lock.withLock {
            try {
                callbacks.forEach { it(errors) }
            } catch (e: Exception) {
                throw Status.fromCode(Status.Code.INTERNAL).withDescription(e.toString()).asException()
            }
        }
        val errorMsg = StringBuilder()
        errors.forEach {
            errorMsg.append(it)
            errorMsg.append('\n')
        }
        if (errors.count() > 0)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(errorMsg.toString()).asException()
        return CompleteDiagramServiceRequest.getDefaultInstance()
    }

    override suspend fun createDiagram(request: CreateDiagramRequest): CreateDiagramResponse {
        try {
            diagramToCim.addFromPb(request.diagram)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateDiagramResponse.getDefaultInstance()
    }


    override suspend fun createDiagramObject(request: CreateDiagramObjectRequest): CreateDiagramObjectResponse {
        try {
            diagramToCim.addFromPb(request.diagramObject)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateDiagramObjectResponse.getDefaultInstance()
    }

}
