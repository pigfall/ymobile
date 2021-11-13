class ClientCfg {
  late String id;
  ClientCfg(String id) {
    this.id = id;
  }
  static Future<List<ClientCfg>> LoadFromDisk() {
    // TODO
    var list = [
      ClientCfg("testId"),
    ];
    return Future.value(
      list,
    );
  }
}
