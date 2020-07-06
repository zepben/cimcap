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

import com.zepben.cimbend.common.extensions.typeNameAndMRID
import com.zepben.cimbend.network.NetworkService
import com.zepben.cimbend.network.model.NetworkProtoToCim
import com.zepben.protobuf.np.*
import io.grpc.Status
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NetworkProducerServer(onComplete: List<(Sequence<String>) -> Unit>? = null) :
    NetworkProducerGrpcKt.NetworkProducerCoroutineImplBase(), CallsBack {

    private val callbacks = mutableListOf<(Sequence<String>) -> Unit>()
    var networkService = NetworkService()
        private set
    private var networkToCim = NetworkProtoToCim(networkService)
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    init {
        onComplete?.forEach { callbacks.add(it) }
    }

    override fun addCallback(callback: (Sequence<String>) -> Unit) {
        callbacks.add(callback)
    }

    override suspend fun createNetwork(request: CreateNetworkRequest): CreateNetworkResponse {
        return CreateNetworkResponse.getDefaultInstance()
    }

    override suspend fun completeNetwork(request: CompleteNetworkRequest): CompleteNetworkResponse {
        val errors = networkService.unresolvedReferences()
            .filter { it.toMrid.isNotEmpty() }
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
        return CompleteNetworkResponse.getDefaultInstance()
    }

    /** IEC-61968 AssetInfo **/
    override suspend fun createCableInfo(request: CreateCableInfoRequest): CreateCableInfoResponse {
        try {
            networkToCim.addFromPb(request.cableInfo)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateCableInfoResponse.getDefaultInstance()
    }

    override suspend fun createOverheadWireInfo(request: CreateOverheadWireInfoRequest): CreateOverheadWireInfoResponse {
        try {
            networkToCim.addFromPb(request.overheadWireInfo)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateOverheadWireInfoResponse.getDefaultInstance()
    }

    /** IEC-61968 Assets **/
    override suspend fun createAssetOwner(request: CreateAssetOwnerRequest): CreateAssetOwnerResponse {
        try {
            networkToCim.addFromPb(request.assetOwner)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateAssetOwnerResponse.getDefaultInstance()
    }

    override suspend fun createPole(request: CreatePoleRequest): CreatePoleResponse {
        try {
            networkToCim.addFromPb(request.pole)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreatePoleResponse.getDefaultInstance()
    }

    override suspend fun createStreetlight(request: CreateStreetlightRequest): CreateStreetlightResponse {
        try {
            networkToCim.addFromPb(request.streetlight)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateStreetlightResponse.getDefaultInstance()
    }

    /** IEC-61968 Common **/
    override suspend fun createLocation(request: CreateLocationRequest): CreateLocationResponse {
        try {
            networkToCim.addFromPb(request.location)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateLocationResponse.getDefaultInstance()
    }

    override suspend fun createOrganisation(request: CreateOrganisationRequest): CreateOrganisationResponse {
        try {
            networkToCim.addFromPb(request.organisation)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateOrganisationResponse.getDefaultInstance()
    }

    /** IEC-61968 Metering **/
    override suspend fun createMeter(request: CreateMeterRequest): CreateMeterResponse {
        try {
            networkToCim.addFromPb(request.meter)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateMeterResponse.getDefaultInstance()
    }

    override suspend fun createUsagePoint(request: CreateUsagePointRequest): CreateUsagePointResponse {
        try {
            networkToCim.addFromPb(request.usagePoint)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateUsagePointResponse.getDefaultInstance()
    }

    /** IEC-61968 Operations **/
    override suspend fun createOperationalRestriction(request: CreateOperationalRestrictionRequest): CreateOperationalRestrictionResponse {
        try {
            networkToCim.addFromPb(request.operationalRestriction)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateOperationalRestrictionResponse.getDefaultInstance()
    }

    /** IEC-61970 base/AuxiliaryEquipment **/
    override suspend fun createFaultIndicator(request: CreateFaultIndicatorRequest): CreateFaultIndicatorResponse {
        try {
            networkToCim.addFromPb(request.faultIndicator)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateFaultIndicatorResponse.getDefaultInstance()
    }

    /** IEC-61970 base/Core **/
    override suspend fun createBaseVoltage(request: CreateBaseVoltageRequest): CreateBaseVoltageResponse {
        try {
            networkToCim.addFromPb(request.baseVoltage)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateBaseVoltageResponse.getDefaultInstance()
    }

    override suspend fun createConnectivityNode(request: CreateConnectivityNodeRequest): CreateConnectivityNodeResponse {
        try {
            networkToCim.addFromPb(request.connectivityNode)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateConnectivityNodeResponse.getDefaultInstance()
    }

    override suspend fun createFeeder(request: CreateFeederRequest): CreateFeederResponse {
        try {
            networkToCim.addFromPb(request.feeder)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateFeederResponse.getDefaultInstance()
    }

    override suspend fun createGeographicalRegion(request: CreateGeographicalRegionRequest): CreateGeographicalRegionResponse {
        try {
            networkToCim.addFromPb(request.geographicalRegion)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateGeographicalRegionResponse.getDefaultInstance()
    }

    override suspend fun createSite(request: CreateSiteRequest): CreateSiteResponse {
        try {
            networkToCim.addFromPb(request.site)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateSiteResponse.getDefaultInstance()
    }

    override suspend fun createSubGeographicalRegion(request: CreateSubGeographicalRegionRequest): CreateSubGeographicalRegionResponse {
        try {
            networkToCim.addFromPb(request.subGeographicalRegion)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateSubGeographicalRegionResponse.getDefaultInstance()
    }

    override suspend fun createSubstation(request: CreateSubstationRequest): CreateSubstationResponse {
        try {
            networkToCim.addFromPb(request.substation)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateSubstationResponse.getDefaultInstance()
    }

    // TODO: This may not be necessary any more as terminals are always sent with their ConductingEquipment
    override suspend fun createTerminal(request: CreateTerminalRequest): CreateTerminalResponse {
        try {
            networkToCim.addFromPb(request.terminal)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateTerminalResponse.getDefaultInstance()
    }

    /** IEC-61970 base/wires **/
    override suspend fun createAcLineSegment(request: CreateAcLineSegmentRequest): CreateAcLineSegmentResponse {
        try {
            request.terminalsList.forEach(networkToCim::addFromPb)
            networkToCim.addFromPb(request.acLineSegment)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateAcLineSegmentResponse.getDefaultInstance()
    }

    override suspend fun createBreaker(request: CreateBreakerRequest): CreateBreakerResponse {
        try {
            request.terminalsList.forEach(networkToCim::addFromPb)
            networkToCim.addFromPb(request.breaker)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateBreakerResponse.getDefaultInstance()
    }

    override suspend fun createDisconnector(request: CreateDisconnectorRequest): CreateDisconnectorResponse {
        try {
            request.terminalsList.forEach(networkToCim::addFromPb)
            networkToCim.addFromPb(request.disconnector)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateDisconnectorResponse.getDefaultInstance()
    }

    override suspend fun createEnergyConsumer(request: CreateEnergyConsumerRequest): CreateEnergyConsumerResponse {
        try {
            request.terminalsList.forEach(networkToCim::addFromPb)
            networkToCim.addFromPb(request.energyConsumer)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateEnergyConsumerResponse.getDefaultInstance()
    }

    override suspend fun createEnergyConsumerPhase(request: CreateEnergyConsumerPhaseRequest): CreateEnergyConsumerPhaseResponse {
        try {
            networkToCim.addFromPb(request.energyConsumerPhase)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateEnergyConsumerPhaseResponse.getDefaultInstance()
    }

    override suspend fun createEnergySource(request: CreateEnergySourceRequest): CreateEnergySourceResponse {
        try {
            request.terminalsList.forEach(networkToCim::addFromPb)
            networkToCim.addFromPb(request.energySource)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateEnergySourceResponse.getDefaultInstance()
    }

    override suspend fun createEnergySourcePhase(request: CreateEnergySourcePhaseRequest): CreateEnergySourcePhaseResponse {
        try {
            networkToCim.addFromPb(request.energySourcePhase)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateEnergySourcePhaseResponse.getDefaultInstance()
    }

    override suspend fun createFuse(request: CreateFuseRequest): CreateFuseResponse {
        try {
            request.terminalsList.forEach(networkToCim::addFromPb)
            networkToCim.addFromPb(request.fuse)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateFuseResponse.getDefaultInstance()
    }

    override suspend fun createJumper(request: CreateJumperRequest): CreateJumperResponse {
        try {
            request.terminalsList.forEach(networkToCim::addFromPb)
            networkToCim.addFromPb(request.jumper)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateJumperResponse.getDefaultInstance()
    }

    override suspend fun createJunction(request: CreateJunctionRequest): CreateJunctionResponse {
        try {
            request.terminalsList.forEach(networkToCim::addFromPb)
            networkToCim.addFromPb(request.junction)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateJunctionResponse.getDefaultInstance()
    }

    override suspend fun createLinearShuntCompensator(request: CreateLinearShuntCompensatorRequest): CreateLinearShuntCompensatorResponse {
        try {
            request.terminalsList.forEach(networkToCim::addFromPb)
            networkToCim.addFromPb(request.linearShuntCompensator)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateLinearShuntCompensatorResponse.getDefaultInstance()
    }

    override suspend fun createPerLengthSequenceImpedance(request: CreatePerLengthSequenceImpedanceRequest): CreatePerLengthSequenceImpedanceResponse {
        try {
            networkToCim.addFromPb(request.perLengthSequenceImpedance)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreatePerLengthSequenceImpedanceResponse.getDefaultInstance()
    }

    override suspend fun createPowerTransformer(request: CreatePowerTransformerRequest): CreatePowerTransformerResponse {
        try {
            request.terminalsList.forEach(networkToCim::addFromPb)
            request.endsList.forEach(networkToCim::addFromPb)
            networkToCim.addFromPb(request.powerTransformer)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreatePowerTransformerResponse.getDefaultInstance()
    }

    override suspend fun createPowerTransformerEnd(request: CreatePowerTransformerEndRequest): CreatePowerTransformerEndResponse {
        try {
            networkToCim.addFromPb(request.powerTransformerEnd)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreatePowerTransformerEndResponse.getDefaultInstance()
    }

    override suspend fun createRatioTapChanger(request: CreateRatioTapChangerRequest): CreateRatioTapChangerResponse {
        try {
            networkToCim.addFromPb(request.ratioTapChanger)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateRatioTapChangerResponse.getDefaultInstance()
    }

    override suspend fun createRecloser(request: CreateRecloserRequest): CreateRecloserResponse {
        try {
            request.terminalsList.forEach(networkToCim::addFromPb)
            networkToCim.addFromPb(request.recloser)
        } catch (e: Exception) {
            logger.debug(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateRecloserResponse.getDefaultInstance()
    }


}
