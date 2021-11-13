import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:ymobile/client_cfg.dart';

class ClientConnList extends StatefulWidget {
  late List<ClientCfg> connCfgs;
  ClientConnList(List<ClientCfg> connCfgs) {
    this.connCfgs = connCfgs;
  }
  @override
  State<StatefulWidget> createState() {
    // TODO: implement createState
    return _ClientConnListState();
  }
}

class _ClientConnListState extends State<ClientConnList> {
  @override
  Widget build(BuildContext context) {
    var children = this.widget.connCfgs.map<Widget>((e) {
      return (ClientConnWidget(e));
    });
    return ListView(
      children: children.toList(),
    );
  }
}

class ClientConnWidget extends StatefulWidget {
  late ClientCfg cfg;
  ClientConnWidget(ClientCfgcfg) {
    this.cfg = cfg;
  }
  @override
  State<StatefulWidget> createState() {
    return _ClientConnWidgetState();
  }
}

class _ClientConnWidgetState extends State<ClientConnWidget> {
  @override
  Widget build(BuildContext context) {
    return Switch(value: false, onChanged: (turnOn) {});
  }
}
