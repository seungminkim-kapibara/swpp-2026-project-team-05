package com.swpp.team5.frameless

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val ScreenBackground = Color(0xFFF7F9FC)
private val Navy = Color(0xFF172A46)
private val Blue = Color(0xFF2F6FED)
private val LightBlue = Color(0xFFEAF1FF)
private val Green = Color(0xFF267A55)
private val LightGreen = Color(0xFFE8F5EE)
private val Orange = Color(0xFFAD5A13)
private val LightOrange = Color(0xFFFFF2E5)

private enum class AppScreen {
    INPUT,
    LOADING,
    OVERVIEW,
    CLAIM_DETAIL
}

private enum class ClaimType {
    SHARED,
    DIFFERENT
}

private data class SourcePassage(
    val outlet: String,
    val excerpt: String,
    val url: String? = null
)

private data class ClaimPreview(
    val type: ClaimType,
    val summary: String,
    val explanation: String,
    val passages: List<SourcePassage>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ScreenBackground
                ) {
                    FrameLESSApp()
                }
            }
        }
    }
}

@Composable
private fun FrameLESSApp() {
    var screen by remember { mutableStateOf(AppScreen.INPUT) }
    var articleUrl by rememberSaveable { mutableStateOf("") }
    var urlError by rememberSaveable { mutableStateOf<String?>(null) }
    var loadingMessage by remember { mutableStateOf("") }
    var selectedClaim by remember { mutableStateOf<ClaimPreview?>(null) }

    LaunchedEffect(screen) {
        if (screen == AppScreen.LOADING) {
            loadingMessage = "기사에서 핵심 사건을 파악하고 있어요."
            delay(900)

            loadingMessage = "같은 사건을 다룬 보도를 비교하고 있어요."
            delay(1100)

            screen = AppScreen.OVERVIEW
        }
    }

    when (screen) {
        AppScreen.INPUT -> {
            InputScreen(
                articleUrl = articleUrl,
                errorMessage = urlError,
                onArticleUrlChange = {
                    articleUrl = it
                    urlError = null
                },
                onAnalyze = {
                    val trimmedUrl = articleUrl.trim()

                    if (!isValidArticleUrl(trimmedUrl)) {
                        urlError = "http:// 또는 https://로 시작하는 기사 주소를 입력해줘."
                    } else {
                        articleUrl = trimmedUrl
                        selectedClaim = null
                        screen = AppScreen.LOADING
                    }
                }
            )
        }

        AppScreen.LOADING -> LoadingScreen(loadingMessage)

        AppScreen.OVERVIEW -> {
            ComparisonOverviewScreen(
                articleUrl = articleUrl,
                onClaimSelected = {
                    selectedClaim = it
                    screen = AppScreen.CLAIM_DETAIL
                },
                onStartOver = {
                    articleUrl = ""
                    urlError = null
                    selectedClaim = null
                    screen = AppScreen.INPUT
                }
            )
        }

        AppScreen.CLAIM_DETAIL -> {
            val fallbackClaim = exampleClaims(articleUrl).first()

            ClaimDetailScreen(
                claim = selectedClaim ?: fallbackClaim,
                onBack = {
                    screen = AppScreen.OVERVIEW
                }
            )
        }
    }
}

@Composable
private fun InputScreen(
    articleUrl: String,
    errorMessage: String?,
    onArticleUrlChange: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    Scaffold(containerColor = ScreenBackground) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "FrameLESS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Blue
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "기사 하나로,\n다른 보도를 함께 보기",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "읽고 있는 뉴스 기사를 넣으면, 같은 사건을 다룬 보도에서 공통으로 언급된 내용과 서로 다르게 강조한 부분을 비교해줘.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF516072)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "비교할 기사 입력",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = articleUrl,
                        onValueChange = onArticleUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("뉴스 기사 URL") },
                        placeholder = {
                            Text("https://news.example.com/article")
                        },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(errorMessage)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue
                        )
                    ) {
                        Text("이 기사 비교하기")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "FrameLESS는 어느 언론사가 옳은지 판정하지 않습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D7885)
            )
        }
    }
}

@Composable
private fun LoadingScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Blue)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "비교 결과를 준비하고 있어요",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF516072)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComparisonOverviewScreen(
    articleUrl: String,
    onClaimSelected: (ClaimPreview) -> Unit,
    onStartOver: () -> Unit
) {
    val claims = remember(articleUrl) { exampleClaims(articleUrl) }
    val sharedClaims = claims.filter { it.type == ClaimType.SHARED }
    val differentClaims = claims.filter { it.type == ClaimType.DIFFERENT }

    Scaffold(
        containerColor = ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "비교 결과",
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                },
                actions = {
                    TextButton(onClick = onStartOver) {
                        Text("새 기사")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "동일 사건 보도 비교",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Text(
                text = "선택된 여러 출처의 보도를 바탕으로, 공통 보도와 서로 다른 강조점을 확인할 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF516072)
            )

            InputArticleCard(articleUrl)

            MultiSourceBriefCard()

            SectionTitle("여러 출처에 공통으로 나타난 보도")

            sharedClaims.forEach { claim ->
                ClaimSummaryCard(
                    claim = claim,
                    onClick = { onClaimSelected(claim) }
                )
            }

            SectionTitle("보도마다 다르게 강조된 내용")

            differentClaims.forEach { claim ->
                ClaimSummaryCard(
                    claim = claim,
                    onClick = { onClaimSelected(claim) }
                )
            }

            SectionTitle("다음 탐색")

            ExplorePreviewCard(
                title = "바로 비교하기",
                description = "같은 사건을 다른 출처는 어떻게 표현했는지 더 확인합니다."
            )

            ExplorePreviewCard(
                title = "다른 쟁점 보기",
                description = "같은 이슈에서 아직 보지 않은 이해관계자나 쟁점을 찾아봅니다."
            )

            ExplorePreviewCard(
                title = "이어서 보기",
                description = "이 이슈를 읽은 사람들이 함께 살펴본 다음 주제를 제안합니다."
            )

            ArchivePreviewCard()

            Text(
                text = "현재 보이는 내용은 UI 검증용 예시입니다. 백엔드 연동 후 실제 기사·문장·출처 링크로 교체됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D7885)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClaimDetailScreen(
    claim: ClaimPreview,
    onBack: () -> Unit
) {
    val category = if (claim.type == ClaimType.SHARED) {
        "여러 출처에 공통으로 나타난 보도"
    } else {
        "출처별 강조점 비교"
    }

    Scaffold(
        containerColor = ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "문장 비교",
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("뒤로")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Blue
            )

            Text(
                text = claim.summary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "비교 기준",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = claim.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF516072)
                    )
                }
            }

            SectionTitle("연결된 원문 문장")

            Text(
                text = "각 출처의 해당 문장을 직접 비교할 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF516072)
            )

            claim.passages.forEachIndexed { index, passage ->
                SourcePassageCard(
                    label = if (index == 0) "입력 기사" else "다른 출처",
                    passage = passage,
                    backgroundColor = if (index == 0) LightBlue else Color.White,
                    accentColor = if (index == 0) Blue else Orange
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LightGreen
                )
            ) {
                Text(
                    text = "이 화면은 어떤 보도가 맞는지 판정하지 않습니다. 사용자가 원문 문장과 출처를 직접 확인할 수 있도록 비교 근거를 보여줍니다.",
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Navy
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InputArticleCard(articleUrl: String) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "비교 기준 기사",
                style = MaterialTheme.typography.labelLarge,
                color = Blue,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = articleHost(articleUrl),
                style = MaterialTheme.typography.titleMedium,
                color = Navy,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = articleUrl,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF667085),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = { uriHandler.openUri(articleUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("입력 기사 원문 열기")
            }
        }
    }
}

@Composable
private fun MultiSourceBriefCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightGreen
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "여러 출처 보도 요약",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Green
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "예시: 선택된 출처들은 정책의 일정과 대상은 함께 언급하지만, 기대 효과·현장 부담·이해관계자 반응에는 서로 다른 비중을 둡니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Navy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "이 요약은 하나의 기사에서 가져온 문장이 아니라, 아래의 공통 보도 항목을 기반으로 구성됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF516072)
            )
        }
    }
}

@Composable
private fun ClaimSummaryCard(
    claim: ClaimPreview,
    onClick: () -> Unit
) {
    val isShared = claim.type == ClaimType.SHARED
    val accentColor = if (isShared) Green else Orange
    val backgroundColor = if (isShared) LightGreen else LightOrange
    val label = if (isShared) {
        "공통 보도"
    } else {
        "강조점 비교"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = claim.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = Navy
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "눌러서 원문 문장 비교하기 →",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun SourcePassageCard(
    label: String,
    passage: SourcePassage,
    backgroundColor: Color,
    accentColor: Color
) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = passage.outlet,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "“${passage.excerpt}”",
                style = MaterialTheme.typography.bodyMedium,
                color = Navy
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (passage.url != null) {
                OutlinedButton(
                    onClick = { uriHandler.openUri(passage.url) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("이 출처의 원문 열기")
                }
            } else {
                Text(
                    text = "MVP 예시 문장입니다. 백엔드 연동 후 실제 원문 링크가 표시됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D7885)
                )
            }
        }
    }
}

@Composable
private fun ExplorePreviewCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF516072)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "다음 Iteration에서 연결 예정",
                style = MaterialTheme.typography.bodySmall,
                color = Blue
            )
        }
    }
}

@Composable
private fun ArchivePreviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightBlue
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "개인 아카이브",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Blue
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "비교한 기사와 확인한 관점을 나중에 다시 볼 수 있도록 저장하는 화면입니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Navy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "다음 Iteration에서 연결 예정",
                style = MaterialTheme.typography.bodySmall,
                color = Blue
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Navy
    )
}

private fun exampleClaims(articleUrl: String): List<ClaimPreview> {
    val inputOutlet = articleHost(articleUrl)

    return listOf(
        ClaimPreview(
            type = ClaimType.SHARED,
            summary = "예시: 정책의 적용 시점과 대상이 여러 보도에서 함께 언급됩니다.",
            explanation = "비슷한 대상·행동·시점을 다룬 문장이 여러 선택 출처에서 발견된 경우입니다.",
            passages = listOf(
                SourcePassage(
                    outlet = inputOutlet,
                    excerpt = "입력 기사의 관련 문장이 이 위치에 표시됩니다.",
                    url = articleUrl
                ),
                SourcePassage(
                    outlet = "다른 출처 A (예시)",
                    excerpt = "같은 사건의 적용 대상과 시점을 언급한 문장이 표시됩니다."
                ),
                SourcePassage(
                    outlet = "다른 출처 B (예시)",
                    excerpt = "동일 내용을 다른 표현으로 다룬 문장이 표시됩니다."
                )
            )
        ),
        ClaimPreview(
            type = ClaimType.SHARED,
            summary = "예시: 관계 기관의 공식 발표과 이후 논의가 함께 보도됩니다.",
            explanation = "표현은 달라도 같은 발표·발언·일정을 지칭하는지 비교하는 항목입니다.",
            passages = listOf(
                SourcePassage(
                    outlet = inputOutlet,
                    excerpt = "입력 기사의 공식 발표 또는 후속 논의 관련 문장이 표시됩니다.",
                    url = articleUrl
                ),
                SourcePassage(
                    outlet = "다른 출처 A (예시)",
                    excerpt = "같은 발표의 세부 일정과 배경을 언급한 문장이 표시됩니다."
                ),
                SourcePassage(
                    outlet = "다른 출처 C (예시)",
                    excerpt = "후속 협의 과정을 언급한 문장이 표시됩니다."
                )
            )
        ),
        ClaimPreview(
            type = ClaimType.DIFFERENT,
            summary = "예시: 입력 기사는 정책의 기대 효과를 상대적으로 더 강조합니다.",
            explanation = "이는 기사가 틀렸다는 판단이 아니라, 같은 사건을 다룬 출처 사이의 강조점 차이를 보여주는 라벨입니다.",
            passages = listOf(
                SourcePassage(
                    outlet = inputOutlet,
                    excerpt = "입력 기사가 기대 효과나 추진 배경을 설명하는 문장이 표시됩니다.",
                    url = articleUrl
                ),
                SourcePassage(
                    outlet = "다른 출처 A (예시)",
                    excerpt = "다른 출처는 현장 부담 또는 이해관계자 반응을 더 비중 있게 다룬 문장이 표시됩니다."
                )
            )
        )
    )
}

private fun isValidArticleUrl(url: String): Boolean {
    val uri = Uri.parse(url)

    return (uri.scheme == "http" || uri.scheme == "https") &&
            !uri.host.isNullOrBlank()
}

private fun articleHost(url: String): String {
    return Uri.parse(url).host ?: url
}