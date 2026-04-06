package server

import kotlinx.coroutines.channels.SendChannel
import openpoker.v1.CreateTournamentRequest
import openpoker.v1.TournamentEvent
import openpoker.v1.TournamentResponse
import openpoker.v1.TournamentServiceServer
import openpoker.v1.UpdateTournamentRequest

class GrpcTournamentServiceServer: TournamentServiceServer {
    override suspend fun Status(
        request: Unit,
        response: SendChannel<TournamentEvent>,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun Create(request: CreateTournamentRequest): TournamentResponse {
        TODO("Not yet implemented")
    }

    override suspend fun Update(request: UpdateTournamentRequest): TournamentResponse {
        TODO("Not yet implemented")
    }
}