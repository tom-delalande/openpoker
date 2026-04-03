@external(erlang, "server_runtime", "start")
fn start_runtime() -> Nil

pub fn main() -> Nil {
  start_runtime()
}
