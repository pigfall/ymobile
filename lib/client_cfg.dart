class ClientCfg {
  late String id;
  ClientCfg(String id) {
    this.id = id;
  }
  static Future<List<ClientCfg>> LoadFromDisk() {
    // TODO
    var list = [
      ClientCfg("tzz toky node"),
    ];
    return Future.value(
      list,
    );
  }
}
