import 'dart:ui';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:tzz_dart_utils/flutter_widgets/switch_button.dart';
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
  ButtonSwitchState? curButtonState;
  ConnStatus connStatus = ConnStatus.DISCONNECTED;
  late List<ClientCfg> cfgs;
  ClientCfg? curConnCfg;
  _AppBodyState() {
    platform.setMethodCallHandler((call) {
      if (call.method == "onConnectFailed") {
        if (this.connStatus == ConnStatus.CONNECING ||
            this.connStatus == ConnStatus.CONNECTED) {
          print(this.connStatus);
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
        }
      } else if (call.method == "onConnected") {
        print("onConnected from flutter");
        this.changeConnStatus(ConnStatus.CONNECTED, null, this.curButtonState);
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
        return Row(
          children: [
            Text(e.id),
            ButtonSwitch((curStatus, buttonState) {
              // mainLogic
              switch (this.connStatus) {
                case ConnStatus.DISCONNECTED:
                  this.connectTunnel(e, buttonState);
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
                                  this.disconnectTunnel(buttonState);
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
                  this.disconnectTunnel(buttonState);
                  break;
                default:
                  throw (Exception(
                      "BUG undefined connStatus ${this.connStatus}"));
              }
            })
          ],
        );
      });
      var list = children.toList();
      // list.insert(0, Text(this.connStatus.toString()));
      return ListView(children: list);
    }
  }

  void changeConnStatus(
      ConnStatus connStatus, ClientCfg? cfg, ButtonSwitchState? buttonState) {
    this.connStatus = connStatus;
    this.curConnCfg = cfg;
    if (connStatus == ConnStatus.CONNECING ||
        connStatus == ConnStatus.DISCONNECTING) {
      this.curButtonState = buttonState;
      buttonState?.changeButtonState(ButtonSwitchStatus.Switching);
    } else if (connStatus == ConnStatus.CONNECTED) {
      this.curButtonState = buttonState;
      buttonState?.changeButtonState(ButtonSwitchStatus.On);
    } else {
      this.curButtonState = null;
      buttonState?.changeButtonState(ButtonSwitchStatus.Off);
    }
  }

  void connectTunnel(ClientCfg cfg, ButtonSwitchState buttonState) {
    this.changeConnStatus(ConnStatus.CONNECING, cfg, buttonState);
//    setState(() {
//      this.connStatus = ConnStatus.CONNECING;
//      this.curConnCfg = cfg;
//    });
    platform.invokeMethod("connect").catchError((e) {
      showDialog(
          context: context,
          builder: (context) {
            return AlertDialog(
              title: Text("Connect failed ${e}"),
              actions: [
                TextButton(
                    onPressed: () {
                      changeConnStatus(
                          ConnStatus.DISCONNECTED, null, buttonState);
                      Navigator.pop(context);
                    },
                    child: Text("close"))
              ],
            );
          });
    }).then((value) {});
  }

  void disconnectTunnel(ButtonSwitchState buttonState) {
    this.changeConnStatus(ConnStatus.DISCONNECTING, null, buttonState);
    showDialog(
        context: context,
        barrierDismissible: false,
        builder: (context) {
          return AlertDialog(
            title: Text("Waiting connection to close"),
          );
        });
    platform.invokeMethod("disconnect").catchError((e) {
      print(e.toString());
    }).then((value) {
      // TODO
      print("disconnect success ");
      this.changeConnStatus(ConnStatus.DISCONNECTED, null, buttonState);
      Navigator.pop(context);
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
