-module(server_runtime).

-export([start/0]).

-define(HOST, {127, 0, 0, 1}).
-define(PORT, 8080).

start() ->
    ok = ensure_apps_started(),
    case openapi_server:start(
           server,
           #{
             transport_opts => #{socket_opts => [{ip, ?HOST}, {port, ?PORT}]},
             logic_handler => server_openapi_logic
            }
          ) of
        {ok, _Pid} ->
            io:format("OpenPoker server listening on http://127.0.0.1:~B~n", [?PORT]),
            wait_forever();
        {error, {already_started, _Pid}} ->
            io:format("OpenPoker server already running on http://127.0.0.1:~B~n", [?PORT]),
            wait_forever();
        {error, Reason} ->
            erlang:error({server_start_failed, Reason})
    end.

ensure_apps_started() ->
    _ = application:ensure_all_started(cowboy),
    _ = application:ensure_all_started(jesse),
    ok.

wait_forever() ->
    receive
        stop ->
            ok
    after infinity ->
        ok
    end.
