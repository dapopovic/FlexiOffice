package com.example.flexioffice.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flexioffice.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val pages =
        listOf(
            OnboardingPage(
                title = stringResource(R.string.onboarding_title_welcome),
                description = stringResource(R.string.onboarding_desc_welcome),
                icon = Icons.Filled.Info,
                color = scheme.primaryContainer,
            ),
            OnboardingPage(
                title = stringResource(R.string.onboarding_title_geofencing),
                description = stringResource(R.string.onboarding_desc_geofencing),
                icon = Icons.Filled.LocationOn,
                color = scheme.secondaryContainer,
            ),
            OnboardingPage(
                title = stringResource(R.string.onboarding_title_notifications),
                description = stringResource(R.string.onboarding_desc_notifications),
                icon = Icons.Filled.Notifications,
                color = scheme.tertiaryContainer,
            ),
        )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    var currentPage by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) { currentPage = pagerState.currentPage }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val p = pages[index]
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ElevatedCard {
                    Box(
                        modifier =
                            Modifier
                                .size(160.dp)
                                .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = p.icon,
                            contentDescription = p.title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(96.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = p.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = p.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Indicator + Controls
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            DotsIndicator(totalDots = pages.size, selectedIndex = currentPage)

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp)
                        .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentPage < pages.lastIndex) {
                    OutlinedButton(onClick = onDone) { Text(stringResource(R.string.onboarding_skip)) }
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }

                Button(onClick = {
                    if (currentPage < pages.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                    } else {
                        onDone()
                    }
                }) {
                    if (currentPage < pages.lastIndex) {
                        Text(stringResource(R.string.onboarding_next))
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = "Done")
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.onboarding_get_started))
                    }
                }
            }
        }
    }
}

@Composable
private fun DotsIndicator(
    totalDots: Int,
    selectedIndex: Int,
) {
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(totalDots) { index ->
            val size = if (index == selectedIndex) 10.dp else 8.dp
            val color =
                if (index ==
                    selectedIndex
                ) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .size(size)
                        .clip(CircleShape)
                        .background(color),
            )
        }
    }
}

private data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
)
