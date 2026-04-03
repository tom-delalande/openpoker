-module(openapi_tournaments_handler).
-moduledoc """
Exposes the following operation IDs:

- `POST` to `/tournaments`, OperationId: `Tournaments_create`:
.


- `DELETE` to `/tournaments/:id`, OperationId: `Tournaments_delete`:
.


- `POST` to `/tournaments/:id/join`, OperationId: `Tournaments_join`:
.


- `POST` to `/tournaments/:id/leave`, OperationId: `Tournaments_leave`:
.


- `GET` to `/tournaments`, OperationId: `Tournaments_list`:
.


- `POST` to `/tournaments/:id/start`, OperationId: `Tournaments_start`:
.


- `GET` to `/tournaments/:id`, OperationId: `Tournaments_status`:
.


""".

-behaviour(cowboy_rest).

-include_lib("kernel/include/logger.hrl").

%% Cowboy REST callbacks
-export([init/2]).
-export([allowed_methods/2]).
-export([content_types_accepted/2]).
-export([content_types_provided/2]).
-export([delete_resource/2]).
-export([is_authorized/2]).
-export([valid_content_headers/2]).
-export([handle_type_accepted/2, handle_type_provided/2]).

-ignore_xref([handle_type_accepted/2, handle_type_provided/2]).

-export_type([class/0, operation_id/0]).

-type class() :: 'tournaments'.

-type operation_id() ::
    'Tournaments_create' %% 
    | 'Tournaments_delete' %% 
    | 'Tournaments_join' %% 
    | 'Tournaments_leave' %% 
    | 'Tournaments_list' %% 
    | 'Tournaments_start' %% 
    | 'Tournaments_status'. %% 


-record(state,
        {operation_id :: operation_id(),
         accept_callback :: openapi_logic_handler:accept_callback(),
         provide_callback :: openapi_logic_handler:provide_callback(),
         api_key_callback :: openapi_logic_handler:api_key_callback(),
         context = #{} :: openapi_logic_handler:context()}).

-type state() :: #state{}.

-spec init(cowboy_req:req(), openapi_router:init_opts()) ->
    {cowboy_rest, cowboy_req:req(), state()}.
init(Req, {Operations, Module}) ->
    Method = cowboy_req:method(Req),
    OperationID = maps:get(Method, Operations, undefined),
    ?LOG_INFO(#{what => "Attempt to process operation",
                method => Method,
                operation_id => OperationID}),
    State = #state{operation_id = OperationID,
                   accept_callback = fun Module:accept_callback/4,
                   provide_callback = fun Module:provide_callback/4,
                   api_key_callback = fun Module:api_key_callback/2},
    {cowboy_rest, Req, State}.

-spec allowed_methods(cowboy_req:req(), state()) ->
    {[binary()], cowboy_req:req(), state()}.
allowed_methods(Req, #state{operation_id = 'Tournaments_create'} = State) ->
    {[<<"POST">>], Req, State};
allowed_methods(Req, #state{operation_id = 'Tournaments_delete'} = State) ->
    {[<<"DELETE">>], Req, State};
allowed_methods(Req, #state{operation_id = 'Tournaments_join'} = State) ->
    {[<<"POST">>], Req, State};
allowed_methods(Req, #state{operation_id = 'Tournaments_leave'} = State) ->
    {[<<"POST">>], Req, State};
allowed_methods(Req, #state{operation_id = 'Tournaments_list'} = State) ->
    {[<<"GET">>], Req, State};
allowed_methods(Req, #state{operation_id = 'Tournaments_start'} = State) ->
    {[<<"POST">>], Req, State};
allowed_methods(Req, #state{operation_id = 'Tournaments_status'} = State) ->
    {[<<"GET">>], Req, State};
allowed_methods(Req, State) ->
    {[], Req, State}.

-spec is_authorized(cowboy_req:req(), state()) ->
    {true | {false, iodata()}, cowboy_req:req(), state()}.
is_authorized(Req, State) ->
    {true, Req, State}.

-spec content_types_accepted(cowboy_req:req(), state()) ->
    {[{binary(), atom()}], cowboy_req:req(), state()}.
content_types_accepted(Req, #state{operation_id = 'Tournaments_create'} = State) ->
    {[
      {<<"application/json">>, handle_type_accepted}
     ], Req, State};
content_types_accepted(Req, #state{operation_id = 'Tournaments_delete'} = State) ->
    {[], Req, State};
content_types_accepted(Req, #state{operation_id = 'Tournaments_join'} = State) ->
    {[], Req, State};
content_types_accepted(Req, #state{operation_id = 'Tournaments_leave'} = State) ->
    {[], Req, State};
content_types_accepted(Req, #state{operation_id = 'Tournaments_list'} = State) ->
    {[], Req, State};
content_types_accepted(Req, #state{operation_id = 'Tournaments_start'} = State) ->
    {[], Req, State};
content_types_accepted(Req, #state{operation_id = 'Tournaments_status'} = State) ->
    {[], Req, State};
content_types_accepted(Req, State) ->
    {[], Req, State}.

-spec valid_content_headers(cowboy_req:req(), state()) ->
    {boolean(), cowboy_req:req(), state()}.
valid_content_headers(Req, #state{operation_id = 'Tournaments_create'} = State) ->
    {true, Req, State};
valid_content_headers(Req, #state{operation_id = 'Tournaments_delete'} = State) ->
    {true, Req, State};
valid_content_headers(Req, #state{operation_id = 'Tournaments_join'} = State) ->
    {true, Req, State};
valid_content_headers(Req, #state{operation_id = 'Tournaments_leave'} = State) ->
    {true, Req, State};
valid_content_headers(Req, #state{operation_id = 'Tournaments_list'} = State) ->
    {true, Req, State};
valid_content_headers(Req, #state{operation_id = 'Tournaments_start'} = State) ->
    {true, Req, State};
valid_content_headers(Req, #state{operation_id = 'Tournaments_status'} = State) ->
    {true, Req, State};
valid_content_headers(Req, State) ->
    {false, Req, State}.

-spec content_types_provided(cowboy_req:req(), state()) ->
    {[{binary(), atom()}], cowboy_req:req(), state()}.
content_types_provided(Req, #state{operation_id = 'Tournaments_create'} = State) ->
    {[
      {<<"application/json">>, handle_type_provided}
     ], Req, State};
content_types_provided(Req, #state{operation_id = 'Tournaments_delete'} = State) ->
    {[
      {<<"application/json">>, handle_type_provided}
     ], Req, State};
content_types_provided(Req, #state{operation_id = 'Tournaments_join'} = State) ->
    {[
      {<<"application/json">>, handle_type_provided}
     ], Req, State};
content_types_provided(Req, #state{operation_id = 'Tournaments_leave'} = State) ->
    {[
      {<<"application/json">>, handle_type_provided}
     ], Req, State};
content_types_provided(Req, #state{operation_id = 'Tournaments_list'} = State) ->
    {[
      {<<"application/json">>, handle_type_provided}
     ], Req, State};
content_types_provided(Req, #state{operation_id = 'Tournaments_start'} = State) ->
    {[
      {<<"application/json">>, handle_type_provided}
     ], Req, State};
content_types_provided(Req, #state{operation_id = 'Tournaments_status'} = State) ->
    {[
      {<<"application/json">>, handle_type_provided}
     ], Req, State};
content_types_provided(Req, State) ->
    {[], Req, State}.

-spec delete_resource(cowboy_req:req(), state()) ->
    {boolean(), cowboy_req:req(), state()}.
delete_resource(Req, State) ->
    {Res, Req1, State1} = handle_type_accepted(Req, State),
    {true =:= Res, Req1, State1}.

-spec handle_type_accepted(cowboy_req:req(), state()) ->
    { openapi_logic_handler:accept_callback_return(), cowboy_req:req(), state()}.
handle_type_accepted(Req, #state{operation_id = OperationID,
                                 accept_callback = Handler,
                                 context = Context} = State) ->
    {Res, Req1, Context1} = Handler(tournaments, OperationID, Req, Context),
    {Res, Req1, State#state{context = Context1}}.

-spec handle_type_provided(cowboy_req:req(), state()) ->
    { openapi_logic_handler:provide_callback_return(), cowboy_req:req(), state()}.
handle_type_provided(Req, #state{operation_id = OperationID,
                                 provide_callback = Handler,
                                 context = Context} = State) ->
    {Res, Req1, Context1} = Handler(tournaments, OperationID, Req, Context),
    {Res, Req1, State#state{context = Context1}}.
