-module(server_openapi_logic).

-behaviour(openapi_logic_handler).

-export([api_key_callback/2, accept_callback/4, provide_callback/4]).

-type context() :: openapi_logic_handler:context().

-spec api_key_callback(openapi_api:operation_id(), binary()) -> {true, context()}.
api_key_callback(_OperationID, _ApiKey) ->
    {true, #{}}.

-spec accept_callback(
        openapi_api:class(),
        openapi_api:operation_id(),
        cowboy_req:req(),
        context()
    ) -> {openapi_logic_handler:accept_callback_return(), cowboy_req:req(), context()}.
accept_callback(_Class, OperationID, Req0, Context0) ->
    case populate(OperationID, Req0, Context0) of
        {ok, Model, Req1, Context1} ->
            handle_accept(OperationID, Model, Req1, Context1);
        {error, Reason, Req1, Context1} ->
            {accept_error_result(Reason), Req1, Context1}
    end.

-spec provide_callback(
        openapi_api:class(),
        openapi_api:operation_id(),
        cowboy_req:req(),
        context()
    ) -> {openapi_logic_handler:provide_callback_return(), cowboy_req:req(), context()}.
provide_callback(_Class, OperationID, Req0, Context0) ->
    case populate(OperationID, Req0, Context0) of
        {ok, Model, Req1, Context1} ->
            {provide_result(OperationID, Model), Req1, Context1};
        {error, Reason, Req1, Context1} ->
            {error_body(Reason), Req1, Context1}
    end.

populate(OperationID, Req0, Context0) ->
    Validator = openapi_api:prepare_validator(),
    case openapi_api:populate_request(OperationID, Req0, Validator) of
        {ok, Model, Req1} ->
            Context1 = maps:merge(Context0, Model),
            {ok, Model, Req1, Context1};
        {error, Reason, Req1} ->
            {error, Reason, Req1, Context0}
    end.

handle_a<ScrollWheelDown>cept('Tournaments_create', Model, Req, Context) ->
    Body = json:encode(maps:get('Tournament', Model, #{})),
    case server_api:create_tournament(Body) of
        {ok, ResponseBody} ->
            {{created, ResponseBody}, Req, Context};
        {error, Message} ->
            {{true, error_body(Message)}, Req, Context}
    end;
handle_accept('Tournaments_delete', Model, Req, Context) ->
    Id = required_id(Model),
    case server_api:delete_tournament(Id) of
        {ok, _} -> {true, Req, Context};
        {error, Message} -> {{true, error_body(Message)}, Req, Context}
    end;
handle_accept('Tournaments_join', Model, Req, Context) ->
    Id = required_id(Model),
    case server_api:join_tournament(Id) of
        {ok, _} -> {true, Req, Context};
        {error, Message} -> {{true, error_body(Message)}, Req, Context}
    end;
handle_accept('Tournaments_leave', Model, Req, Context) ->
    Id = required_id(Model),
    case server_api:leave_tournament(Id) of
        {ok, _} -> {true, Req, Context};
        {error, Message} -> {{true, error_body(Message)}, Req, Context}
    end;
handle_accept('Tournaments_start', Model, Req, Context) ->
    Id = required_id(Model),
    case server_api:start_tournament(Id) of
        {ok, _} -> {true, Req, Context};
        {error, Message} -> {{true, error_body(Message)}, Req, Context}
    end;
handle_accept(_OperationID, _Model, Req, Context) ->
    {false, Req, Context}.

provide_result('Tables_connect', Model) ->
    Id = required_id(Model),
    case server_api:tables_connect(Id) of
        {ok, Body} -> Body;
        {error, Message} -> Message
    end;
provide_result('Tournaments_create', Model) ->
    Body = json:encode(maps:get('Tournament', Model, #{})),
    case server_api:create_tournament(Body) of
        {ok, ResponseBody} -> ResponseBody;
        {error, Message} -> error_body(Message)
    end;
provide_result('Tournaments_delete', Model) ->
    Id = required_id(Model),
    case server_api:delete_tournament(Id) of
        {ok, _} -> <<>>;
        {error, Message} -> error_body(Message)
    end;
provide_result('Tournaments_join', Model) ->
    Id = required_id(Model),
    case server_api:join_tournament(Id) of
        {ok, _} -> <<>>;
        {error, Message} -> error_body(Message)
    end;
provide_result('Tournaments_leave', Model) ->
    Id = required_id(Model),
    case server_api:leave_tournament(Id) of
        {ok, _} -> <<>>;
        {error, Message} -> error_body(Message)
    end;
provide_result('Tournaments_list', _Model) ->
    case server_api:list_tournaments() of
        {ok, Body} -> Body;
        {error, Message} -> error_body(Message)
    end;
provide_result('Tournaments_start', Model) ->
    Id = required_id(Model),
    case server_api:start_tournament(Id) of
        {ok, _} -> <<>>;
        {error, Message} -> error_body(Message)
    end;
provide_result('Tournaments_status', Model) ->
    Id = required_id(Model),
    case server_api:tournament_status(Id) of
        {ok, Body} -> Body;
        {error, Message} -> error_body(Message)
    end;
provide_result(_OperationID, _Model) ->
    error_body(<<"unsupported operation">>).

required_id(Model) ->
    maps:get('id', Model, <<>>).

accept_error_result(Reason) ->
    {true, error_body(Reason)}.

error_body(Reason) when is_binary(Reason) ->
    json:encode(#{<<"error">> => Reason});
error_body(Reason) when is_list(Reason) ->
    error_body(iolist_to_binary(Reason));
error_body(Reason) ->
    error_body(iolist_to_binary(io_lib:format("~p", [Reason]))).
