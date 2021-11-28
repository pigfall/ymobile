import 'package:ymobile/cfg_manager.dart';
import 'package:json_annotation/json_annotation.dart';

part 'client_cfg.g.dart';

@JsonSerializable()
class ClientCfg {
  static const String FieldServerHost = "serverHost";
  static const String FieldServerPort = "serverPort";
  static const String FieldId = "id";
  final String id;
  final String serverHost;
  final String serverPort;
  ClientCfg(
      {required this.id, required this.serverHost, required this.serverPort});
  static Future<List<ClientCfg>> LoadFromDisk() {
    return CfgManager.AllCfgs();
  }

  Map<String, dynamic> toJson() => _$ClientCfgToJson(this);
}
