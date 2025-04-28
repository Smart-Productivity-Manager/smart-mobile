import 'dart:async';
import 'package:flutter/services.dart';
import '../models/app_usage_stats.dart';

class AppUsageService {
  static const platform =
      MethodChannel('com.smart.smart_productivity/app_usage');

  Future<List<AppUsageStats>> getAppUsageStats() async {
    try {
      // Vérifier les permissions avant de récupérer les statistiques
      final bool hasPermission = await platform.invokeMethod('checkUsageStatsPermission');
      if (!hasPermission) {
        throw PlatformException(
          code: 'PERMISSION_DENIED',
          message: "L'application n'a pas la permission d'accéder aux statistiques d'utilisation",
        );
      }

      final List<dynamic> result =
          await platform.invokeMethod('getAppUsageStats');
      return result.map((data) {
        final Map<String, dynamic> typedData = {};
        (data as Map).forEach((key, value) {
          typedData[key.toString()] = value;
        });
        return AppUsageStats.fromJson(typedData);
      }).toList();
    } on PlatformException catch (e) {
      print('Erreur lors de la récupération des statistiques : ${e.message}');
      rethrow;
    } catch (e) {
      print('Erreur inattendue : $e');
      rethrow;
    }
  }

  Future<bool> isAppBlocked(String packageName) async {
    try {
      final bool result = await platform
          .invokeMethod('isAppBlocked', {'packageName': packageName});
      return result;
    } on PlatformException catch (e) {
      print('Erreur lors de la vérification du blocage : ${e.message}');
      return false;
    }
  }

  Future<bool> setAppBlocked(String packageName, bool blocked) async {
    try {
      final bool result = await platform.invokeMethod(
        'setAppBlocked',
        {
          'packageName': packageName,
          'blocked': blocked,
        },
      );
      return result;
    } on PlatformException catch (e) {
      print('Erreur lors du blocage de l\'application : ${e.message}');
      return false;
    }
  }
}
