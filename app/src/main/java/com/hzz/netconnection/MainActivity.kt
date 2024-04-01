package com.hzz.netconnection

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hzz.netconnection.MainViewModel.Companion.Factory
import com.hzz.netconnection.ui.theme.NetConnectionTheme
import com.hzz.netconnection.webserver.FileServer
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var fileServer: FileServer
    private val permissions = listOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    )

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileServer = FileServer(8888, this)
        fileServer.start()
        setContent {
            requestPermissions(permissions.toTypedArray(), 0)
            NetConnectionTheme {
                val lazyColumnState = rememberLazyListState()
                val mainViewModel: MainViewModel = viewModel(factory = Factory)
                val pingIp = mainViewModel.pingIp.collectAsState()
                val url = mainViewModel.url.collectAsState()
                val logs = mainViewModel.logList
                val audioInfoList = mainViewModel.audioInfoList
                val isUrlPrepared = mainViewModel.isUrlPrepared.collectAsState()
                val isHttps = mainViewModel.isHttps.collectAsState()
                val wifiIpAddress = mainViewModel.getIpInfo(this@MainActivity)
                var showDialog by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val pagerState = rememberPagerState { 2 }
                    val coroutineScope = rememberCoroutineScope()
                    // local net information
                    TextField(
                        value = "Local Server initialized with\n " +
                                "$wifiIpAddress:${fileServer.port} ",
                        onValueChange = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    // ping
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = pingIp.value,
                            onValueChange = { mainViewModel.updateServerIp(it) },
                            placeholder = { Text(text = "www.google.com", color = Color.Gray) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                        )
                        Button(
                            onClick = {
                                mainViewModel.ping()
                            },
                            enabled = pingIp.value.isNotEmpty()
                        ) {
                            Text("Ping")
                        }
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    // url setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            value = url.value,
                            placeholder = { Text(text = "192.168.0.2:8080", color = Color.Gray) },
                            enabled = !isUrlPrepared.value,
                            onValueChange = { mainViewModel.updateUrl(it) },
                            leadingIcon = {
                                Box(modifier = Modifier
                                    .padding(4.dp)
                                    .background(Color.LightGray)
                                    .clickable { mainViewModel.updateSchema(!isHttps.value) }
                                ) {
                                    if (isHttps.value) Text("https://")
                                    else Text(text = "http://")
                                }
                            }
                        )
                        Button(
                            onClick = {
                                if (!isUrlPrepared.value) {
                                    mainViewModel.updateUrlState(isPrepared = true)
                                    mainViewModel.getAudiosInfo()
                                } else mainViewModel.reset()
                            },
                            enabled = url.value.isNotEmpty()
                        ) {
                            if (!isUrlPrepared.value) Text("Confirm")
                            else Text("Reset")
                        }
                    }
                    Spacer(modifier = Modifier.size(16.dp))


                    LaunchedEffect(key1 = logs.size) {
                        launch {
                            lazyColumnState.animateScrollToItem(logs.size)
                        }
                    }
                    HorizontalPager(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        state = pagerState
                    ) {
                        if (it == 0) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray),
                                state = lazyColumnState
                            ) {
                                items(logs) { log ->
                                    Text(text = log)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                items(audioInfoList) { audioInfo ->
                                    val fileName = audioInfo.link.linkToFileName()
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp)
                                    ) {
                                        Row(
                                            Modifier
                                                .fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = fileName,
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .padding(end = 16.dp, start = 16.dp)
                                                    .weight(1f)
                                            )
                                            Icon(
                                                painter = painterResource(id = R.drawable.baseline_download_24),
                                                contentDescription = "download",
                                                modifier = Modifier
                                                    .clickable {
                                                        mainViewModel.downloadAudio(
                                                            audioInfo,
                                                            this@MainActivity
                                                        )
                                                    }
                                                    .padding(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    }

                    AnimatedVisibility(visible = true) {
                        Row {
                            if (pagerState.currentPage == 0) {
                                Button(
                                    onClick = {
                                        mainViewModel.clearLog()
                                    },
                                    enabled = logs.isNotEmpty()
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_clear_24),
                                        contentDescription = "clear log"
                                    )
                                }
                            } else {
                                Button(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(1)
                                        }
                                        mainViewModel.getAudiosInfo()
                                    },
                                    enabled = isUrlPrepared.value
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_sync_24),
                                        contentDescription = "refresh"
                                    )
                                }
                                Button(
                                    onClick = { showDialog = true },
                                    enabled = isUrlPrepared.value
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_checklist_24),
                                        contentDescription = "download all audio"
                                    )
                                }
                            }

                        }

                        if (showDialog) {
                            DownloadAlertDialog(
                                onDismissRequest = { showDialog = false },
                                onConfirmation = {
                                    mainViewModel.autoSync(this@MainActivity)
                                    showDialog = false
                                },
                                dialogTitle = "Warning",
                                dialogText = "Download all of the audios, be sure your phone have enough storage." +
                                        " Check logging page for download location",
                                icon = Icons.Default.Warning
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DownloadAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
) {
    AlertDialog(
        icon = {
            Icon(icon, contentDescription = "Example Icon")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}