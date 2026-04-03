import "@typespec/http";

using Http;
@service(#{ title: "OpenPoker" })
namespace OpenPoker;

model Tournament {
  id: string;
}

@error
model Error {
  code: int32;
  message: string;
}

@route("/tournaments")
@tag("Tournaments")
interface Tournaments {
  @get list(): Tournament[] | Error;
  @post create(@body body: Tournament): Tournament | Error;
  @delete delete(@path id: string): void | Error;
  @post @route("{id}/join") join(@path id: string): void | Error;
  @post @route("{id}/leave") leave(@path id: string): void | Error;
  @post @route("{id}/start") start(@path id: string): void | Error;
  @get status(@path id: string): void | Error;
}

@route("/tables")
@tag("Tables")
interface Tables {
  @get @route("{id}/ws") connect(@path id: string): void | Error;
}
