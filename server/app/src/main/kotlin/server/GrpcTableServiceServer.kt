package server

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import openpoker.v1.TableAction
import openpoker.v1.TableEvent
import openpoker.v1.TableServiceServer

class GrpcTableServiceServer: TableServiceServer {
    override suspend fun Connect(
        request: ReceiveChannel<TableAction>,
        response: SendChannel<TableEvent>,
    ) {
        TODO("Not yet implemented")
    }
}