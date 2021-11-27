import 'package:ymobile/cfg_manager.dart';

class ClientCfg {
  static const String FieldServerHost = "serverHost";
  static const String FieldServerPort = "serverPort";
  static const String FieldId = "id";
  final String id;
  final String servserHost;
  final String servserPort;
  ClientCfg(
      {required this.id, required this.servserHost, required this.servserPort});
  static Future<List<ClientCfg>> LoadFromDisk() {
    return CfgManager.AllCfgs();
  }
}
