package com.sudsmobile.feature.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.shared.ui.SudsCustomerScreen
import com.sudsmobile.shared.ui.SudsSecondaryTopBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ServicesScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onBookService: (String) -> Unit,
) {
    val catalogViewModel: ProductsCatalogViewModel = koinViewModel()
    val catalogState by catalogViewModel.catalogState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        catalogViewModel.loadCatalog()
    }

    SudsCustomerScreen(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            ServicesHeader(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                ServicesCatalogContent(
                catalogState = catalogState,
                onRetryCatalog = catalogViewModel::loadCatalog,
                onBookService = onBookService,
            )

                ServicesTipCard()
            }
        }
    }
}

@Composable
private fun ServicesHeader(onBack: () -> Unit) {
    SudsSecondaryTopBar(
        title = "Serviços",
        eyebrow = "Cuidado automóvel",
        onBack = onBack,
    )
}

@Composable
private fun ServicesCatalogContent(
    catalogState: ProductCatalogUiState,
    onRetryCatalog: () -> Unit,
    onBookService: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (catalogState) {
            ProductCatalogUiState.Idle,
            ProductCatalogUiState.Loading -> ServiceCatalogLoadingCard()

            is ProductCatalogUiState.Loaded -> {
                catalogState.services.forEach { service ->
                    ServiceCatalogCard(
                        service = service,
                        onClick = { onBookService(service.id) },
                    )
                }
            }

            ProductCatalogUiState.Empty -> ServiceCatalogStatusCard(
                title = "Sem serviços disponíveis",
                body = "O catálogo ainda não tem serviços ativos para marcação.",
                onRetry = onRetryCatalog,
            )

            is ProductCatalogUiState.Error -> ServiceCatalogStatusCard(
                title = "Não foi possível carregar serviços",
                body = catalogState.message,
                onRetry = onRetryCatalog,
            )
        }
    }
}

@Composable
private fun ServiceCatalogLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 2.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "A carregar serviços",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Estamos a consultar o catálogo em tempo real.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ServiceCatalogStatusCard(
    title: String,
    body: String,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Tentar novamente", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ServiceCatalogCard(
    service: ProductServiceUi,
    onClick: () -> Unit,
) {
    BookingServiceCard(
        service = service,
        selected = false,
        onSelected = onClick,
    )
}

@Composable
private fun PriceBlock(label: String, price: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = price,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ServicesExtrasContent(
    catalogState: ProductCatalogUiState,
    onRetryCatalog: () -> Unit,
) {
    when (catalogState) {
        ProductCatalogUiState.Idle,
        ProductCatalogUiState.Loading -> ServiceExtrasLoadingGrid()

        is ProductCatalogUiState.Loaded -> {
            if (catalogState.extras.isEmpty()) {
                ServiceCatalogStatusCard(
                    title = "Sem extras disponíveis",
                    body = "O catálogo ainda não tem extras ativos para marcação.",
                    onRetry = onRetryCatalog,
                )
            } else {
                ExtrasGrid(catalogState.extras)
            }
        }

        ProductCatalogUiState.Empty -> ServiceCatalogStatusCard(
            title = "Sem extras disponíveis",
            body = "Quando houver extras ativos no catálogo, aparecem aqui.",
            onRetry = onRetryCatalog,
        )

        is ProductCatalogUiState.Error -> ServiceCatalogStatusCard(
            title = "Não foi possível carregar extras",
            body = catalogState.message,
            onRetry = onRetryCatalog,
        )
    }
}

@Composable
private fun ServiceExtrasLoadingGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtraLoadingCard(Modifier.weight(1f))
                ExtraLoadingCard(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExtraLoadingCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(132.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
                contentColor = MaterialTheme.colorScheme.tertiary,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(13.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    strokeWidth = 2.dp,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "A carregar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Preço em tempo real",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExtrasGrid(extras: List<ProductExtraUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        extras.chunked(2).forEach { rowExtras ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowExtras.forEach { extra ->
                    ExtraCard(extra, Modifier.weight(1f))
                }
                if (rowExtras.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ExtraCard(extra: ProductExtraUi, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(132.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            ServiceIconBadge(extra.icon, size = 48.dp, cornerRadius = 14.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = extra.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = extra.price,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ServicesTipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.20f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(22.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Dica",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Escolha o serviço ideal. No passo seguinte pode personalizar a marcação com extras opcionais.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ServiceIconBadge(
    icon: ImageVector,
    size: Dp,
    cornerRadius: Dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
        contentColor = MaterialTheme.colorScheme.tertiary,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(size / 4),
        )
    }
}
