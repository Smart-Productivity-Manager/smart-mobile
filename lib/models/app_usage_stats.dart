class AppUsageStats {
  final String packageName;
  final Duration usageTime;
  final DateTime lastUsed;

  AppUsageStats({
    required this.packageName,
    required this.usageTime,
    required this.lastUsed,
  });

  factory AppUsageStats.fromJson(Map<String, dynamic> json) {
    return AppUsageStats(
      packageName: json['packageName'] as String,
      usageTime: Duration(milliseconds: json['usageTime'] as int),
      lastUsed: DateTime.fromMillisecondsSinceEpoch(json['lastUsed'] as int),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'packageName': packageName,
      'usageTime': usageTime.inMilliseconds,
      'lastUsed': lastUsed.millisecondsSinceEpoch,
    };
  }
}
