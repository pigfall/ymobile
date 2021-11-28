import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'dart:io';
import 'package:ymobile/cfg_manager.dart';
import 'package:ymobile/client_cfg.dart';

class ImportCfgPageRoute extends StatelessWidget {
  final Function(ClientCfg) onAddCfg;
  final List<ClientCfg> existsCfgs;
  const ImportCfgPageRoute(
      {Key? key, required this.onAddCfg, required this.existsCfgs})
      : super(key: key);
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Import config'),
      ),
      body: ImportCfgPageBody(
        onAddCfg: onAddCfg,
        existsCfgs: existsCfgs,
      ),
    );
  }
}

class ImportCfgPageBody extends StatefulWidget {
  final Function(ClientCfg) onAddCfg;
  final List<ClientCfg> existsCfgs;
  const ImportCfgPageBody(
      {Key? key, required this.onAddCfg, required this.existsCfgs})
      : super(key: key);
  @override
  State<StatefulWidget> createState() {
    return _ImportCfgPageBody();
  }
}

class _ImportCfgPageBody extends State<ImportCfgPageBody> {
  final configName = TextEditingController();
  final serverHost = TextEditingController();
  final serverPort = TextEditingController();
  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        children: [
          TextField(
            controller: configName,
            decoration: InputDecoration(
              border: OutlineInputBorder(),
              hintText: 'config name',
            ),
          ),
          TextField(
            controller: serverHost,
            decoration: InputDecoration(
              border: OutlineInputBorder(),
              hintText: 'server host',
            ),
          ),
          TextField(
            controller: serverPort,
            decoration: InputDecoration(
              border: OutlineInputBorder(),
              hintText: 'server port',
            ),
          ),
          TextButton(
            child: Text("import"),
            onPressed: () async {
              var cfgName = configName.text;
              for (var existsCfg in this.widget.existsCfgs) {
                if (existsCfg.id == cfgName) {
                  showDialog(
                      context: context,
                      builder: (context) {
                        return AlertDialog(
                          title: Text(
                              "Config name has existed, change the config name"),
                        );
                      });
                }
                return;
              }
              var p = await CfgManager.GetCfgDirPath();
              var saveFilePath = '${p}/${cfgName}.json';
              var saveFile = await File(saveFilePath);
              var clientCfg = ClientCfg(
                  id: cfgName,
                  serverHost: serverHost.text,
                  serverPort: serverPort.text);
              var saveObj = {
                ClientCfg.FieldId: cfgName,
                ClientCfg.FieldServerHost: serverHost.text,
                ClientCfg.FieldServerPort: serverPort.text
              };
              await saveFile.writeAsString(jsonEncode(saveObj));
              this.widget.onAddCfg(clientCfg);
              Navigator.pop(context);
            },
          ),
        ],
      ),
    );
  }
}

Future<String> get LocalPath async {
  final directory = await getApplicationDocumentsDirectory();

  return directory.path;
}
