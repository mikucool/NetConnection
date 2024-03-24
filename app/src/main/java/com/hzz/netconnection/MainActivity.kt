package com.hzz.netconnection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hzz.netconnection.ui.theme.NetConnectionTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetConnectionTheme {
                val lazyColumnState = rememberLazyListState()
                val mainViewModel: MainViewModel = viewModel()
                val serverIp = mainViewModel.serverIp.collectAsState()
                val url = mainViewModel.url.collectAsState()
                val logs = mainViewModel.logList
                val fileList = mainViewModel.fileList
                val isConnected = mainViewModel.isConnected.collectAsState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val pagerState = rememberPagerState { 2 }
                    val coroutineScope = rememberCoroutineScope()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = serverIp.value,
                            onValueChange = { mainViewModel.updateServerIp(it) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                        )
                        Button(onClick = { mainViewModel.pingServer() }) {
                            Text("Ping")
                        }
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    TextField(
                        value = mainViewModel.getIpInfo(this@MainActivity) + "",
                        onValueChange = {},
                        enabled = false,
                        label = { Text(text = "local internet information") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    TextField(
                        enabled = !isConnected.value,
                        value = url.value,
                        label = { Text(text = "input schema, ip and port") },
                        onValueChange = { mainViewModel.updateUrl(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                    ) {
                        Button(
                            onClick = {
                                if (!isConnected.value) mainViewModel.httpConnect()
                                else mainViewModel.reset()
                            }) {
                            if (!isConnected.value) Text(text = "connect")
                            else Text(text = "reset")
                        }
                        Spacer(modifier = Modifier.size(16.dp))
                        Button(onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    1,
                                    animationSpec = spring(stiffness = 10f)
                                )
                            }
                            mainViewModel.toyaudio()
                        }) {
                            Text(text = "toyaudio")
                        }
                    }

                    LaunchedEffect(key1 = logs.size) {
                        launch {
                            lazyColumnState.animateScrollToItem(logs.size)
                        }
                    }
                    HorizontalPager(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
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
                                items(fileList) { fileName ->
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
                                                        mainViewModel.downloadFile(
                                                            fileName,
                                                            AndroidDownloader(this@MainActivity)
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

                }
            }
        }
    }
}

