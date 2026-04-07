package server

import openpoker.v1.AuthResponse
import openpoker.v1.CreateTournamentRequest
import openpoker.v1.GameServiceServer
import openpoker.v1.GetTablesRequest
import openpoker.v1.GetTablesResponse

class GrpcGameServiceServer : GameServiceServer {
    override suspend fun Auth(request: CreateTournamentRequest): AuthResponse {
        TODO("Not yet implemented")
    }

    override suspend fun GetTables(request: GetTablesRequest): GetTablesResponse {
        TODO("Not yet implemented")
    }
}