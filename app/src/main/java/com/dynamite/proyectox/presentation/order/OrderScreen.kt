package com.dynamite.proyectox.presentation.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.dynamite.proyectox.common.Resource
import com.dynamite.proyectox.domain.model.Product
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    navController: NavController,
    state: OrderScreenState,
    cartItemCount: Int,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onAddProductClicked: (Product) -> Unit,
    onNavigateToCart: () -> Unit
) {
    val filteredProducts = if (state.productsResource is Resource.Success) {
        state.productsResource.data?.filter { product ->
            (state.selectedCategory == null || product.category.equals(state.selectedCategory, ignoreCase = true)) &&
                    (state.searchQuery.isBlank() || product.name.contains(state.searchQuery, ignoreCase = true))
        } ?: emptyList()
    } else {
        emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Menú")
                        if (state.tableNumber != null && state.tableNumber != 0) {
                            Text(
                                "Mesa ${state.tableNumber}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge(
                                    modifier = Modifier.offset(x = (-15).dp, y = 15.dp), // Ajuste de posición
                                    containerColor = Color(0xFFE53935) // Color rojo como en la imagen
                                ) {
                                    Text(
                                        text = cartItemCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp, // Fuente más pequeña
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToCart) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = "Carrito"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ... (El resto del contenido de OrderScreen no cambia)
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar producto") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.categories) { category ->
                    Button(
                        onClick = { onCategorySelected(category) },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.selectedCategory == category) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (state.selectedCategory == category) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(category)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (state.productsResource) {
                    is Resource.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is Resource.Error -> {
                        Text(
                            text = state.productsResource.message ?: "Error al cargar productos",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    }
                    is Resource.Success -> {
                        if (filteredProducts.isEmpty()) {
                            Text(
                                text = if (state.productsResource.data.isNullOrEmpty()) "No hay productos disponibles." else "No hay productos que coincidan.",
                                modifier = Modifier.align(Alignment.Center),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                items(filteredProducts) { product ->
                                    ProductListItem(
                                        product = product,
                                        onAddClick = { onAddProductClicked(product) }
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductListItem(
    product: Product,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = com.dynamite.proyectox.presentation.cart.formatCurrency(product.price),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFA726)
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Add +", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OrderScreenPreview() {
    MaterialTheme {
        val sampleProducts = listOf(
            Product("1", "Hamburguesa Clásica", "Deliciosa hamburguesa", BigDecimal("12.00"), "BURGERS", null),
            Product("2", "Hamburguesa BBQ", "Con salsa BBQ", BigDecimal("15.00"), "BURGERS", null)
        )
        val sampleCategories = listOf("BURGERS", "BEBIDAS", "ENTRADAS")

        OrderScreen(
            navController = rememberNavController(),
            state = OrderScreenState(
                tableNumber = 1,
                productsResource = Resource.Success(sampleProducts),
                searchQuery = "",
                selectedCategory = "BURGERS",
                categories = sampleCategories,
                isLoading = false
            ),
            cartItemCount = 3,
            onSearchQueryChanged = { },
            onCategorySelected = { },
            onAddProductClicked = { },
            onNavigateToCart = { }
        )
    }
}
