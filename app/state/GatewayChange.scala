package state

import model.GateState

sealed abstract class GatewayChange

case object FlushAllGateways extends GatewayChange
case class ToState(state: GateState.Value, gatewayId: Long, cultistId: Long) extends GatewayChange
case class ChangedGateway(gatewayId: Long, cultistId: Long) extends GatewayChange
case class ChangedGateways(cultistId: Long) extends GatewayChange