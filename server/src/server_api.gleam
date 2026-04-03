pub fn create_tournament(body: String) -> Result(String, String) {
  Ok(
    "{"
    <> "\"id\":\"tournament-123\","
    <> "\"name\":\"Stub Tournament\","
    <> "\"status\":\"created\","
    <> "\"source_body\":"
    <> body
    <> "}",
  )
}

pub fn delete_tournament(_id: String) -> Result(String, String) {
  Ok("")
}

pub fn join_tournament(_id: String) -> Result(String, String) {
  Ok("")
}

pub fn leave_tournament(_id: String) -> Result(String, String) {
  Ok("")
}

pub fn list_tournaments() -> Result(String, String) {
  Ok(
    "["
    <> "{"
    <> "\"id\":\"tournament-123\","
    <> "\"name\":\"Stub Tournament\","
    <> "\"status\":\"waiting\""
    <> "},"
    <> "{"
    <> "\"id\":\"tournament-456\","
    <> "\"name\":\"Second Stub Tournament\","
    <> "\"status\":\"running\""
    <> "}"
    <> "]",
  )
}

pub fn start_tournament(_id: String) -> Result(String, String) {
  Ok("")
}

pub fn tournament_status(id: String) -> Result(String, String) {
  Ok(
    "{"
    <> "\"id\":\""
    <> id
    <> "\","
    <> "\"name\":\"Stub Tournament\","
    <> "\"status\":\"waiting\""
    <> "}",
  )
}

pub fn tables_connect(id: String) -> Result(String, String) {
  Error(
    "{\"error\":\"websocket not implemented yet\",\"table_id\":\""
    <> id
    <> "\"}",
  )
}
