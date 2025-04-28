import 'dart:async';
import 'package:flutter/foundation.dart';
import '../services/app_usage_service.dart';
import '../models/app_usage_stats.dart';

class FocusModeViewModel extends ChangeNotifier {
  final AppUsageService _appUsageService = AppUsageService();
  List<AppUsageStats> _appStats = [];
  bool _isFocusModeActive = false;
  Timer? _updateTimer;

  List<AppUsageStats> get appStats => _appStats;
  bool get isFocusModeActive => _isFocusModeActive;

  FocusModeViewModel() {
    _startMonitoring();
  }

  void _startMonitoring() {
    _updateTimer = Timer.periodic(const Duration(minutes: 1), (_) {
      updateAppStats();
    });
  }

  Future<void> updateAppStats() async {
    _appStats = await _appUsageService.getAppUsageStats();
    notifyListeners();
  }

  Future<void> toggleFocusMode() async {
    _isFocusModeActive = !_isFocusModeActive;

    // Bloquer les applications non productives quand le mode focus est activé
    if (_isFocusModeActive) {
      for (var app in _appStats) {
        if (_isNonProductiveApp(app.packageName)) {
          await _appUsageService.setAppBlocked(app.packageName, true);
        }
      }
    } else {
      // Débloquer toutes les applications quand le mode focus est désactivé
      for (var app in _appStats) {
        await _appUsageService.setAppBlocked(app.packageName, false);
      }
    }

    notifyListeners();
  }

  bool _isNonProductiveApp(String packageName) {
    // Liste des applications considérées comme non productives
    final nonProductiveApps = [
      'com.facebook.katana',
      'com.facebook.orca',
      'com.facebook.lite',
      'com.instagram.android',
      'com.instagram.lite',
      'com.meta.threads',
      'com.twitter.android',
      'com.snapchat.android',
      'com.zhiliaoapp.musically',
      'com.zhiliaoapp.musically.go',
      'com.whatsapp',
      'com.google.android.youtube',
      'com.linkedin.android',
      'com.pinterest',
      'org.telegram.messenger',
      'com.discord',
    ];

    return nonProductiveApps.contains(packageName);
  }

  @override
  void dispose() {
    _updateTimer?.cancel();
    super.dispose();
  }
}
