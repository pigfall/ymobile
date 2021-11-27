import 'dart:async';
import 'package:async/async.dart' show CancelableOperation;
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
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel("yingying");
  int _counter = 0;
  bool turnOn = false;
  Future? prevConn = Future.value();
  String status = "";
  String curConnCfgId = "";
  bool pending = false;

  void _incrementCounter() {
    setState(() {
      // This call to setState tells the Flutter framework that something has
      // changed in this State, which causes it to rerun the build method below
      // so that the display can reflect the updated values. If we changed
      // _counter without calling setState(), then the build method would not be
      // called again, and so nothing would appear to happen.
      _counter++;
    });
  }

  Future _connectToTunnel() {
    return platform.invokeMethod("connectTunnel");
  }

  Future fakeConnect() {
    return Future.delayed(Duration(seconds: 3), () {
      print("sleep over");
      return "Test";
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
    // This method is rerun every time setState is called, for instance as done
    // by the _incrementCounter method above.
    //
    // The Flutter framework has been optimized to make rerunning build methods
    // fast, so that you can just rebuild anything that needs updating rather
    // than having to individually change instances of widgets.
    return Scaffold(
      appBar: AppBar(
        // Here we take the value from the MyHomePage object that was created by
        // the App.build method, and use it to set our appbar title.
        title: Text(widget.title),
      ),
      body: Center(
        child: AppBody(),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _connectToTunnel,
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
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
