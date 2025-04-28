import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../viewmodels/focus_mode_viewmodel.dart';
import '../models/app_usage_stats.dart';

class FocusModeView extends StatelessWidget {
  const FocusModeView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mode Concentration'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: Consumer<FocusModeViewModel>(
        builder: (context, viewModel, child) {
          return Column(
            children: [
              // Switch pour activer/désactiver le mode concentration
              Padding(
                padding: const EdgeInsets.all(16.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Mode Concentration',
                      style:
                          TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    Switch(
                      value: viewModel.isFocusModeActive,
                      onChanged: (value) => viewModel.toggleFocusMode(),
                    ),
                  ],
                ),
              ),

              // Liste des applications avec leur temps d'utilisation
              Expanded(
                child: ListView.builder(
                  itemCount: viewModel.appStats.length,
                  itemBuilder: (context, index) {
                    final AppUsageStats stat = viewModel.appStats[index];
                    return ListTile(
                      leading: const Icon(Icons.apps),
                      title: Text(stat.packageName),
                      subtitle: Text(
                        'Temps d\'utilisation: ${stat.usageTime.inMinutes} minutes',
                      ),
                      trailing: Text(
                        'Dernière utilisation:\n${stat.lastUsed.toString().split('.')[0]}',
                        textAlign: TextAlign.end,
                      ),
                    );
                  },
                ),
              ),
            ],
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Provider.of<FocusModeViewModel>(context, listen: false)
              .updateAppStats();
        },
        child: const Icon(Icons.refresh),
      ),
    );
  }
}
