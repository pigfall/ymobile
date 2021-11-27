import 'dart:async';
import 'dart:io';
import 'dart:convert';

import 'package:tzz_dart_utils/flutter_widgets/filesystem.dart';
import 'package:tzz_dart_utils/http/lib.dart';
import 'package:ymobile/client_cfg.dart';

class CfgManager {
  static const yingyingCfgDirName = "yingyingCfg";
  static Future<String> GetCfgDirPath() async {
    var dirPath = await FileSystem.AppDictoryPath();
    return '$dirPath/$yingyingCfgDirName';
  }

  static Future<List<ClientCfg>> AllCfgs() async {
    var cfgDirPath = await GetCfgDirPath();
    await Directory(cfgDirPath).create();
    var fileList = await Directory(cfgDirPath).list().toList();
    var cfgs = <ClientCfg>[];
    Iterable<File> files = fileList.whereType<File>();
    for (var file in files) {
      var content = await file.readAsString();
      var obj = jsonDecode(content);
      print(
        ClientCfg(
            id: obj[ClientCfg.FieldId],
            servserHost: obj[ClientCfg.FieldServerHost],
            servserPort: obj[ClientCfg.FieldServerPort]),
      );
      cfgs.add(
        ClientCfg(
            id: obj[ClientCfg.FieldId],
            servserHost: obj[ClientCfg.FieldServerHost],
            servserPort: obj[ClientCfg.FieldServerPort]),
      );
    }
    return cfgs;
  }
}

//return ClientCfg(
//    id: obj[ClientCfg.FieldId],
//    servserHost: obj[ClientCfg.FieldServerHost],
//    servserPort: obj[ClientCfg.FieldServerPort]);
