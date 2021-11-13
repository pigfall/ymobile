import 'dart:ui';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'client_cfg.dart';

class AppBody extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _AppBodyState();
  }
}

enum ConnStatus {
  DISCONNECTED,
  DISCONNECTING,
  CONNECING,
  CONNECTED,
}

class _AppBodyState extends State<AppBody> {
  static const platform = MethodChannel("ying");
  bool hasloadedConnCfgs = false;
  bool needMannulTriggerLoad = false;
  ConnStatus connStatus = ConnStatus.DISCONNECTED;
  late List<ClientCfg> cfgs;
  ClientCfg? curConnCfg;
  _AppBodyState() {
    platform.setMethodCallHandler((call) {
      if (call.method == "onConnectFailed") {
        showDialog(
            context: context,
            builder: (context) {
              return AlertDialog(
                title: Text("Connected Failed"),
                content: Text("${call}"),
                actions: [
                  TextButton(
                      onPressed: () {
                        Navigator.pop(context);
                      },
                      child: Text("close"))
                ],
              );
            });
        setState(() {
          this.connStatus = ConnStatus.DISCONNECTED;
        });
      } else {
        print("BUG undefined method ${call.method}");
        throw (Exception("BUG undefined method ${call.method}"));
      }
      return Future.value(0);
    });
  }
  @override
  Widget build(BuildContext context) {
    if (!this.hasloadedConnCfgs) {
      return this.onUnloadedCfgs();
    } else {
      var children = this.cfgs.map<Widget>((e) {
        return TextButton(
            onPressed: () {
              // mainLogic
              switch (this.connStatus) {
                case ConnStatus.DISCONNECTED:
                  this.connectTunnel(e);
                  break;
                case ConnStatus.CONNECING:
                  // TODO Dialog to select　if disconnect the current connection
                  showDialog(
                      context: context,
                      builder: (context) {
                        return AlertDialog(
                          title: Text("Disconnect connection?"),
                          actions: [
                            TextButton(
                                onPressed: () {
                                  // TODO
                                  Navigator.pop(context);
                                  this.disconnectTunnel();
                                },
                                child: Text("Yes")),
                            TextButton(
                                onPressed: () {
                                  Navigator.pop(context);
                                },
                                child: Text("No")),
                          ],
                        );
                      });
                  break;
                case ConnStatus.DISCONNECTING:
                  throw (Exception("BUG unreachable"));
                case ConnStatus.CONNECTED:
                  this.disconnectTunnel();
                  break;
                default:
                  throw (Exception(
                      "BUG undefined connStatus ${this.connStatus}"));
              }
            },
            child: Text(e.id));
      });
      var list = children.toList();
      list.insert(0, Text(this.connStatus.toString()));
      return ListView(children: list);
    }
  }

  void connectTunnel(ClientCfg cfg) {
    setState(() {
      this.connStatus = ConnStatus.CONNECING;
      this.curConnCfg = cfg;
    });
    platform.invokeMethod("connect").catchError((e) {
      showDialog(
          context: context,
          builder: (context) {
            return AlertDialog(
              title: Text("Connect failed ${e}"),
              actions: [
                TextButton(
                    onPressed: () {
                      setState(() {
                        this.connStatus = ConnStatus.DISCONNECTED;
                        this.curConnCfg = null;
                      });
                      Navigator.pop(context);
                    },
                    child: Text("close"))
              ],
            );
          });
    }).then((value) {
      //setState(() {
      //  this.connStatus = ConnStatus.CONNECTED;
      //});
    });
  }

  void disconnectTunnel() {
    setState(() {
      this.connStatus = ConnStatus.DISCONNECTING;
      this.curConnCfg = null;
    });
    showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: Text("Waiting connection to close"),
          );
        });
    Future.delayed(Duration(seconds: 3), () {
      setState(() {
        this.connStatus = ConnStatus.DISCONNECTED;
      });
      Navigator.of(context, rootNavigator: true).pop();
    });
  }

  Widget onUnloadedCfgs() {
    if (this.needMannulTriggerLoad) {
      return TextButton(
          onPressed: () {
            setState(() {
              this.needMannulTriggerLoad = false;
            });
          },
          child: Text("LoadFailed click to try again"));
    } else {
      //Future.delayed(Duration(seconds: 3), () {
      //  setState(() {
      //    this.needMannulTriggerLoad = true;
      //  });
      //});
      ClientCfg.LoadFromDisk().catchError((error, stackTrace) {
        this.needMannulTriggerLoad = true;
      }).then((cfgs) {
        this.cfgs = cfgs;
        setState(() {
          this.hasloadedConnCfgs = true;
        });
      });
      return Text("Loading");
    }
  }
}

class AppBodyLoadingCfgs extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Text("loading");
  }
}

class AppBodyLoadedCfgs extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    throw UnimplementedError();
  }
}
