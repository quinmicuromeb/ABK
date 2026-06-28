@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.abk.kernel.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abk.kernel.R
import com.abk.kernel.data.model.BuildProgress
import com.abk.kernel.data.model.BuildStatus
import com.abk.kernel.data.model.WorkflowRun

@Composable
fun BuildStatusSectionContent(
    buildStatus: BuildStatus,
    buildProgress: BuildProgress,
    currentRun: WorkflowRun?,
    activeBuildRuns: List<WorkflowRun>,
    cancellingWorkflowRunIds: Set<Long>,
    progressAnimLabel: String,
    onCancelRun: (Long) -> Unit
) {
    val context = LocalContext.current

    BuildStatusIndicator(
        buildStatus = buildStatus,
        buildProgress = buildProgress,
        parallelCount = activeBuildRuns.size
    )

    if (currentRun != null && buildProgress.totalSteps > 0) {
        Spacer(Modifier.height(8.dp))
        val animatedProgress by animateFloatAsState(
            targetValue = (buildProgress.percent / 100f).coerceIn(0f, 1f),
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = progressAnimLabel
        )
        ShimmerLinearProgress(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(
                R.string.status_steps_complete,
                buildProgress.completedSteps,
                buildProgress.totalSteps
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val showSingleRunAction = activeBuildRuns.size <= 1
    currentRun?.takeIf { showSingleRunAction }?.let { run ->
        Spacer(Modifier.height(4.dp))
        BuildRunActionRow(
            run = run,
            cancelling = run.id in cancellingWorkflowRunIds,
            context = context,
            onCancel = { onCancelRun(run.id) }
        )
    }

    if (activeBuildRuns.size > 1) {
        Text(
            stringResource(R.string.status_parallel_workflows_desc, activeBuildRuns.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BuildStatusIndicator(
    buildStatus: BuildStatus,
    buildProgress: BuildProgress,
    parallelCount: Int
) {
    when (buildStatus) {
        BuildStatus.IDLE -> StatusIndicatorRow(
            Icons.Default.HourglassEmpty,
            stringResource(R.string.status_no_running_build),
            false
        )
        BuildStatus.QUEUED -> StatusIndicatorRow(
            Icons.Default.Queue,
            if (parallelCount > 1) {
                stringResource(R.string.status_parallel_build_waiting_runner, parallelCount)
            } else {
                stringResource(R.string.status_build_waiting_runner)
            },
            false
        )
        BuildStatus.IN_PROGRESS -> Row(verticalAlignment = Alignment.CenterVertically) {
            LoadingIndicator(Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("${buildProgress.percent}% · ${buildProgress.currentStep}")
        }
        BuildStatus.SUCCESS -> StatusIndicatorRow(
            Icons.Default.CheckCircle,
            stringResource(R.string.status_recent_build_success),
            false
        )
        BuildStatus.FAILURE -> StatusIndicatorRow(
            Icons.Default.Error,
            stringResource(R.string.status_recent_build_failed),
            true
        )
        BuildStatus.CANCELLED -> StatusIndicatorRow(
            Icons.Default.Cancel,
            stringResource(R.string.status_build_cancelled),
            true
        )
    }
}

@Composable
private fun StatusIndicatorRow(icon: ImageVector, text: String, isError: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            icon, null,
            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BuildRunActionRow(
    run: WorkflowRun,
    cancelling: Boolean,
    context: Context,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(run.htmlUrl))
                    )
                }
            },
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(R.string.status_view_details, run.runNumber),
                style = MaterialTheme.typography.labelMedium
            )
        }
        if (isBuildRunActive(run)) {
            TextButton(
                onClick = onCancel,
                enabled = !cancelling,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                if (cancelling) {
                    LoadingIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    if (cancelling) {
                        stringResource(R.string.status_cancelling)
                    } else {
                        stringResource(R.string.status_cancel)
                    }
                )
            }
        }
    }
}

private fun isBuildRunActive(run: WorkflowRun): Boolean =
    run.status in setOf("queued", "waiting", "requested", "pending", "in_progress")
