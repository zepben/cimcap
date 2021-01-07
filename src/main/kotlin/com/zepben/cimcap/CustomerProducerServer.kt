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

import com.zepben.evolve.services.common.extensions.typeNameAndMRID
import com.zepben.evolve.services.customer.CustomerService
import com.zepben.evolve.services.customer.translator.CustomerProtoToCim
import com.zepben.protobuf.cp.*
import io.grpc.Status
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CustomerProducerServer(onComplete: List<(Sequence<String>) -> Unit>? = null) : CustomerProducerGrpcKt.CustomerProducerCoroutineImplBase(), CallsBack {

    private val callbacks = mutableListOf<(Sequence<String>) -> Unit>()
    var customerService = CustomerService()
        private set
    private var customerToCim = CustomerProtoToCim(customerService)
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    init {
        onComplete?.forEach { callbacks.add(it) }
    }

    override fun addCallback(callback: (Sequence<String>) -> Unit) {
        callbacks.add(callback)
    }

    override suspend fun createCustomerService(request: CreateCustomerServiceRequest): CreateCustomerServiceRequest {
        return CreateCustomerServiceRequest.getDefaultInstance()
    }

    override suspend fun completeCustomerService(request: CompleteCustomerServiceRequest): CompleteCustomerServiceRequest {
        val errors = customerService.unresolvedReferences()
            .map { "${it.from.typeNameAndMRID()} was missing a reference to  ${it.resolver.toClass.simpleName} ${it.toMrid}" }
        try {
            callbacks.forEach { it(errors) }
        } catch (e: Exception) {
            throw Status.fromCode(Status.Code.INTERNAL).withDescription(e.toString()).asException()
        }
        val errorMsg = StringBuilder()
        errors.forEach {
            errorMsg.append(it)
            errorMsg.append('\n')
        }
        if (errors.count() > 0)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(errorMsg.toString()).asException()
        return CompleteCustomerServiceRequest.getDefaultInstance()
    }

    override suspend fun createOrganisation(request: CreateOrganisationRequest): CreateOrganisationResponse {
        try {
            customerToCim.addFromPb(request.organisation)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateOrganisationResponse.getDefaultInstance()
    }

    override suspend fun createCustomer(request: CreateCustomerRequest): CreateCustomerResponse {
        try {
            customerToCim.addFromPb(request.customer)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateCustomerResponse.getDefaultInstance()
    }

    override suspend fun createCustomerAgreement(request: CreateCustomerAgreementRequest): CreateCustomerAgreementResponse {
        try {
            customerToCim.addFromPb(request.customerAgreement)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateCustomerAgreementResponse.getDefaultInstance()
    }


    override suspend fun createPricingStructure(request: CreatePricingStructureRequest): CreatePricingStructureResponse {
        try {
            customerToCim.addFromPb(request.pricingStructure)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreatePricingStructureResponse.getDefaultInstance()
    }

    override suspend fun createTariff(request: CreateTariffRequest): CreateTariffResponse {
        try {
            customerToCim.addFromPb(request.tariff)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateTariffResponse.getDefaultInstance()
    }
}
