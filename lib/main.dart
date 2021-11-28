import 'dart:async';
import 'package:tzz_dart_utils/flutter_widgets/switch_button.dart';
import 'package:async/async.dart' show CancelableOperation;
import 'package:ymobile/client_cfg.dart';
import 'package:ymobile/import_page.dart';
import 'appBody.dart';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:tzz_dart_utils/flutter_widgets/switch_button.dart'
    show ButtonSwitch;

const TITLE = "Smile";
void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: TITLE,
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Palette.kToDark,
      ),
      home: const MyHomePage(title: TITLE),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageStateV2();
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel("ying");
  bool hasloadedConnCfgs = false;
  bool needMannulTriggerLoad = false;
  ButtonSwitchState? curButtonState;
  ConnStatus connStatus = ConnStatus.DISCONNECTED;
  late List<ClientCfg> cfgs;
  ClientCfg? curConnCfg;

  Widget buildBody() {
    if (!this.hasloadedConnCfgs) {
      return this.onUnloadedCfgs();
    } else {
      var children = this.cfgs.map<Widget>((e) {
        return Row(
          children: [
            Text(e.id),
            ButtonSwitch(
                onPress: (curStatus, buttonState) {
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
                },
                offDesc: "Disconnected",
                switchingDesc: "Switching",
                onDesc: "Connected")
          ],
        );
      });
      var list = children.toList();
      // list.insert(0, Text(this.connStatus.toString()));
      return ListView(children: list);
    }
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

  _MyHomePageState() {
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

  void changeConnStatus(
      ConnStatus connStatus, ClientCfg? cfg, ButtonSwitchState? buttonState) {
    print("change conn status ");
    print(buttonState);
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

  Future<String> sleep() {
    return Future.delayed(Duration(seconds: 10), () {
      print("sleep over");
      return "Test";
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: buildBody(),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.push(context, MaterialPageRoute(builder: (context) {
            return ImportCfgPageRoute(
              onAddCfg: (ClientCfg cfg) {
                setState(() {
                  this.cfgs.insert(0, cfg);
                });
              },
            );
          }));
        },
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
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
        print(error);
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
}

class Palette {
  static const MaterialColor kToDark = const MaterialColor(
    0xff343f26, // 0% comes in here, this will be color picked if no shade is selected when defining a Color property which doesn’t require a swatch.
    const <int, Color>{
      50: const Color(0xff343f26), //10%
      100: const Color(0xff4a553e), //20%
      200: const Color(0xff616a56), //30%
      300: const Color(0xff777f6e), //40%
      400: const Color(0xff8e9587), //50%
      500: const Color(0xffa5aa9f), //60%
      600: const Color(0xffbbbfb7), //70%
      700: const Color(0xffd2d4cf), //80%
      800: const Color(0xff170907), //90%
      900: const Color(0xff000000), //100%
    },
  );
} // you can

class _MyHomePageStateV2 extends State<MyHomePage> {
  static const String DISCONNECTED = "DISCONNECTED";
  static const platform = MethodChannel("ying");
  bool hasloadedConnCfgs = false;
  bool needMannulTriggerLoad = false;
  ConnStatus connStatus = ConnStatus.DISCONNECTED;
  late List<ClientCfg> cfgs;
  ClientCfg? curConnCfg;

  bool isOnDisconnectedStatus() {
    return this.curConnCfg == null;
  }

  bool isCurrentClientCfg(String toComparedCfgId) {
    return this.curConnCfg?.id == toComparedCfgId;
  }

  Widget buildBody() {
    if (!this.hasloadedConnCfgs) {
      return this.onUnloadedCfgs();
    } else {
      var children = this.cfgs.map<Widget>((clientCfg) {
        return Row(
          children: [
            Text(clientCfg.id),
            TextButton(onPressed: () {
              if (isOnDisconnectedStatus()) {
                connectTunnel(clientCfg);
                return;
              }

              switch (connStatus) {
                case ConnStatus.DISCONNECTED:
                  throw (Exception("BUG unreachable"));
                case ConnStatus.CONNECING:
                  disconnectTunnel();
                  break;

                case ConnStatus.CONNECTED:
                  disconnectTunnel();
                  break;

                case ConnStatus.DISCONNECTING:
                  // DO NOTHING
                  break;
                default:
                  throw (Exception("undefined connStatus ${connStatus}"));
              }
            }, child: () {
              if (isOnDisconnectedStatus()) {
                return Text(DISCONNECTED);
              }
              if (!isCurrentClientCfg(clientCfg.id)) {
                return Text(DISCONNECTED);
              }
              return Text(connStatus.toString());
            }())
          ],
        );
      });
      var list = children.toList();
      // list.insert(0, Text(this.connStatus.toString()));
      return ListView(children: list);
    }
  }

  void disconnectTunnel() {
    setState(() {
      connStatus = ConnStatus.DISCONNECTING;
    });
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
      setState(() {
        this.connStatus = ConnStatus.DISCONNECTED;
        curConnCfg = null;
      });
      Navigator.pop(context);
    });
  }

  _MyHomePageStateV2() {
    platform.setMethodCallHandler((call) {
      if (call.method == "onConnectFailed") {
        if (this.connStatus == ConnStatus.CONNECING ||
            this.connStatus == ConnStatus.CONNECTED) {
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
            this.curConnCfg == null;
          });
        }
      } else if (call.method == "onConnected") {
        print("onConnected from flutter");
        setState(() {
          this.connStatus = ConnStatus.CONNECTED;
        });
      } else {
        print("BUG undefined method ${call.method}");
        throw (Exception("BUG undefined method ${call.method}"));
      }
      return Future.value(0);
    });
  }

  Future<String> sleep() {
    return Future.delayed(Duration(seconds: 10), () {
      print("sleep over");
      return "Test";
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: buildBody(),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.push(context, MaterialPageRoute(builder: (context) {
            return ImportCfgPageRoute(
              onAddCfg: (ClientCfg cfg) {
                setState(() {
                  this.cfgs.insert(0, cfg);
                });
              },
            );
          }));
        },
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
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
        print(error);
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

  void connectTunnel(ClientCfg cfg) {
    setState(() {
      this.curConnCfg = cfg;
      this.connStatus = ConnStatus.CONNECING;
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
                        connStatus = ConnStatus.DISCONNECTED;
                        curConnCfg = null;
                      });
                      Navigator.pop(context);
                    },
                    child: Text("close"))
              ],
            );
          });
    }).then((value) {});
  }
}
