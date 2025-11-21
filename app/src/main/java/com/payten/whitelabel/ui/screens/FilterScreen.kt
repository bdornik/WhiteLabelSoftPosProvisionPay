package com.payten.whitelabel.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.enums.TransactionSortType
import com.payten.whitelabel.enums.TransactionSource
import com.payten.whitelabel.enums.TransactionStatusFilterType
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.theme.MyriadPro
import java.text.SimpleDateFormat
import java.util.*

/**
 * Filter screen for transactions using SharedPreferences
 *
 * @param sharedPreferences SharedPreferences instance
 * @param onApplyFilter Callback when the filter application button is clicked
 * @param onNavigateBack Callback when back button is clicked
 */
@Composable
fun FilterScreen(
    sharedPreferences: KsPrefs,
    onApplyFilter: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ENGLISH) }

    // Load current filters from SharedPreferences
    var selectedType by remember {
        mutableStateOf(
            sharedPreferences.pull(SharedPreferencesKeys.FILTER_TYPE, TransactionSource.POS.ordinal)
        )
    }
    var selectedStatus by remember {
        mutableStateOf(
            sharedPreferences.pull(SharedPreferencesKeys.FILTER_STATUS, TransactionStatusFilterType.ALL.ordinal)
        )
    }
    var selectedSort by remember {
        mutableStateOf(
            sharedPreferences.pull(SharedPreferencesKeys.FILTER_SORT, TransactionSortType.DateDesc.ordinal)
        )
    }
    var dateFrom by remember {
        mutableStateOf(sharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, ""))
    }
    var dateTo by remember {
        mutableStateOf(sharedPreferences.pull(SharedPreferencesKeys.DATE_TO, ""))
    }

    // Section expansion states
    var dateExpanded by remember { mutableStateOf(true) }
    var typeExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    
    val activeFiltersCount = remember(dateFrom, dateTo, selectedStatus, selectedSort, selectedType) {
        var count = 0
        if (dateFrom.isNotEmpty()) count++
        if (dateTo.isNotEmpty()) count++
        if (selectedStatus != TransactionStatusFilterType.ALL.ordinal) count++
        if (selectedSort != TransactionSortType.DateDesc.ordinal) count++
        if (selectedType != TransactionSource.POS.ordinal) count++
        count
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        FilterHeader(
            onNavigateBack = onNavigateBack
        )

        if (activeFiltersCount > 0) {
            ActiveFiltersSection(
                dateFrom = dateFrom,
                dateTo = dateTo,
                selectedType = selectedType,
                selectedStatus = selectedStatus,
                selectedSort = selectedSort,
                onRemoveDateFrom = {
                    dateFrom = ""
                    sharedPreferences.remove(SharedPreferencesKeys.DATE_FROM)
                },
                onRemoveDateTo = {
                    dateTo = ""
                    sharedPreferences.remove(SharedPreferencesKeys.DATE_TO)
                },
                onRemoveType = {
                    selectedType = TransactionSource.POS.ordinal
                    sharedPreferences.remove(SharedPreferencesKeys.FILTER_TYPE)
                },
                onRemoveStatus = {
                    selectedStatus = TransactionStatusFilterType.ALL.ordinal
                    sharedPreferences.remove(SharedPreferencesKeys.FILTER_STATUS)
                },
                onRemoveSort = {
                    selectedSort = TransactionSortType.DateDesc.ordinal
                    sharedPreferences.remove(SharedPreferencesKeys.FILTER_SORT)
                },
                onClearAll = {  
                    selectedType = TransactionSource.POS.ordinal
                    selectedStatus = TransactionStatusFilterType.ALL.ordinal
                    selectedSort = TransactionSortType.DateDesc.ordinal
                    dateFrom = ""
                    dateTo = ""

                    sharedPreferences.remove(SharedPreferencesKeys.FILTER_TYPE)
                    sharedPreferences.remove(SharedPreferencesKeys.FILTER_STATUS)
                    sharedPreferences.remove(SharedPreferencesKeys.FILTER_SORT)
                    sharedPreferences.remove(SharedPreferencesKeys.DATE_FROM)
                    sharedPreferences.remove(SharedPreferencesKeys.DATE_TO)
                }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            ExpandableFilterSection(
                title = stringResource(R.string.filter_date_label),
                isExpanded = dateExpanded,
                onToggle = { dateExpanded = !dateExpanded }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateTimeField(
                        label = stringResource(R.string.filter_date_from),
                        value = dateFrom,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(year, month, day)

                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                                            calendar.set(Calendar.MINUTE, minute)
                                            val formattedDate = dateFormat.format(calendar.time)
                                            dateFrom = formattedDate
                                            sharedPreferences.push(SharedPreferencesKeys.DATE_FROM, formattedDate)
                                        },
                                        0, 0, true
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )

                    DateTimeField(
                        label = stringResource(R.string.filter_date_to),
                        value = dateTo,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(year, month, day)

                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                                            calendar.set(Calendar.MINUTE, minute)
                                            val formattedDate = dateFormat.format(calendar.time)
                                            dateTo = formattedDate
                                            sharedPreferences.push(SharedPreferencesKeys.DATE_TO, formattedDate)
                                        },
                                        0, 0, true
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )
                }
            }

            ExpandableFilterSection(
                title = stringResource(R.string.filter_transaction_type),
                isExpanded = typeExpanded,
                onToggle = { typeExpanded = !typeExpanded }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButtonOption(
                        text = stringResource(R.string.filter_type_pos),
                        selected = selectedType == TransactionSource.POS.ordinal,
                        onClick = { selectedType = TransactionSource.POS.ordinal }
                    )
                    RadioButtonOption(
                        text = stringResource(R.string.filter_type_ips),
                        selected = selectedType == TransactionSource.IPS.ordinal,
                        onClick = { selectedType = TransactionSource.IPS.ordinal }
                    )
                }
            }

            ExpandableFilterSection(
                title = stringResource(R.string.filter_transaction_status),
                isExpanded = statusExpanded,
                onToggle = { statusExpanded = !statusExpanded }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButtonOption(
                        text = stringResource(R.string.filter_status_all),
                        selected = selectedStatus == TransactionStatusFilterType.ALL.ordinal,
                        onClick = { selectedStatus = TransactionStatusFilterType.ALL.ordinal }
                    )
                    RadioButtonOption(
                        text = stringResource(R.string.filter_status_accepted),
                        selected = selectedStatus == TransactionStatusFilterType.ACCEPTED.ordinal,
                        onClick = { selectedStatus = TransactionStatusFilterType.ACCEPTED.ordinal }
                    )
                    RadioButtonOption(
                        text = stringResource(R.string.filter_status_rejected),
                        selected = selectedStatus == TransactionStatusFilterType.REJECTED.ordinal,
                        onClick = { selectedStatus = TransactionStatusFilterType.REJECTED.ordinal }
                    )
                    RadioButtonOption(
                        text = stringResource(R.string.filter_status_voided),
                        selected = selectedStatus == TransactionStatusFilterType.VOID.ordinal,
                        onClick = { selectedStatus = TransactionStatusFilterType.VOID.ordinal }
                    )
                }
            }

            ExpandableFilterSection(
                title = stringResource(R.string.filter_sort_order),
                isExpanded = sortExpanded,
                onToggle = { sortExpanded = !sortExpanded }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButtonOption(
                        text = stringResource(R.string.filter_sort_date_desc),
                        selected = selectedSort == TransactionSortType.DateDesc.ordinal,
                        onClick = { selectedSort = TransactionSortType.DateDesc.ordinal }
                    )
                    RadioButtonOption(
                        text = stringResource(R.string.filter_sort_date_asc),
                        selected = selectedSort == TransactionSortType.DateAsc.ordinal,
                        onClick = { selectedSort = TransactionSortType.DateAsc.ordinal }
                    )
                    RadioButtonOption(
                        text = stringResource(R.string.filter_sort_amount_asc),
                        selected = selectedSort == TransactionSortType.AmountAsc.ordinal,
                        onClick = { selectedSort = TransactionSortType.AmountAsc.ordinal }
                    )
                    RadioButtonOption(
                        text = stringResource(R.string.filter_sort_amount_desc),
                        selected = selectedSort == TransactionSortType.AmountDesc.ordinal,
                        onClick = { selectedSort = TransactionSortType.AmountDesc.ordinal }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        Button(
            onClick = {
                sharedPreferences.push(SharedPreferencesKeys.FILTER_TYPE, selectedType)
                sharedPreferences.push(SharedPreferencesKeys.FILTER_STATUS, selectedStatus)
                sharedPreferences.push(SharedPreferencesKeys.FILTER_SORT, selectedSort)

                onApplyFilter()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEB3223)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.filter_apply_button),
                fontSize = 16.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun FilterHeader(
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(onClick = onNavigateBack)

        Text(
            text = stringResource(R.string.filter_title),
            fontSize = 20.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.width(48.dp))
    }
}



@Composable
private fun ExpandableFilterSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(if (isExpanded) 180f else 0f),
                    tint = Color.Gray
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFiltersSection(
    dateFrom: String,
    dateTo: String,
    selectedType: Int,
    selectedStatus: Int,
    selectedSort: Int,
    onRemoveDateFrom: () -> Unit,
    onRemoveDateTo: () -> Unit,
    onRemoveType: () -> Unit,
    onRemoveStatus: () -> Unit,
    onRemoveSort: () -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(size = 12.dp)
                )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.filter_selected_filters),
                fontSize = 12.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )

            TextButton(
                onClick = onClearAll,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = stringResource(R.string.filter_clear_all),
                    fontSize = 12.sp,
                    fontFamily = MyriadPro,
                    color = Color(0xFFEB3223)
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (dateFrom.isNotEmpty()) {
                FilterChip(
                    text = "Datum od ${dateFrom.substringBefore(" ")}",
                    onRemove = onRemoveDateFrom
                )
            }

            if (dateTo.isNotEmpty()) {
                FilterChip(
                    text = "Do ${dateTo.substringBefore(" ")}",
                    onRemove = onRemoveDateTo
                )
            }

            if (selectedType != TransactionSource.POS.ordinal) {
                FilterChip(
                    text = if (selectedType == TransactionSource.IPS.ordinal) "IPS" else "POS",
                    onRemove = onRemoveType
                )
            }

            if (selectedStatus != TransactionStatusFilterType.ALL.ordinal) {
                val statusText = when (selectedStatus) {
                    TransactionStatusFilterType.ACCEPTED.ordinal -> stringResource(R.string.filter_status_accepted)
                    TransactionStatusFilterType.REJECTED.ordinal -> stringResource(R.string.filter_status_rejected)
                    TransactionStatusFilterType.VOID.ordinal -> stringResource(R.string.filter_status_voided)
                    else -> ""
                }
                if (statusText.isNotEmpty()) {
                    FilterChip(
                        text = statusText,
                        onRemove = onRemoveStatus
                    )
                }
            }

            if (selectedSort != TransactionSortType.DateDesc.ordinal) {
                val sortText = when (selectedSort) {
                    TransactionSortType.DateAsc.ordinal -> stringResource(R.string.filter_sort_date_asc)
                    TransactionSortType.AmountAsc.ordinal -> stringResource(R.string.filter_sort_amount_asc)
                    TransactionSortType.AmountDesc.ordinal -> stringResource(R.string.filter_sort_amount_desc)
                    else -> ""
                }
                if (sortText.isNotEmpty()) {
                    FilterChip(
                        text = sortText,
                        onRemove = onRemoveSort
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEB3223)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontFamily = MyriadPro,
                color = Color(0xFFEB3223)
            )

            Icon(
                painter = painterResource(id = R.drawable.close),
                contentDescription = null,
                tint = Color(0xFFEB3223),
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onRemove)
            )
        }
    }
}

@Composable
private fun DateTimeField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = MyriadPro,
            color = Color.Gray
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value.ifEmpty { stringResource(R.string.filter_select_date) },
                    fontSize = 14.sp,
                    fontFamily = MyriadPro,
                    color = if (value.isNotEmpty()) Color.Black else Color.Gray
                )

                Icon(
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RadioButtonOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontFamily = MyriadPro,
            color = Color.Black
        )

        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFFEB3223),
                unselectedColor = Color.Gray
            )
        )
    }
}

/**
 * Count active filters from SharedPreferences
 */
fun countActiveFilters(sharedPreferences: KsPrefs): Int {
    var count = 0
    if (sharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, "").isNotEmpty()) count++
    if (sharedPreferences.pull(SharedPreferencesKeys.DATE_TO, "").isNotEmpty()) count++
    if (sharedPreferences.pull(SharedPreferencesKeys.FILTER_STATUS, TransactionStatusFilterType.ALL.ordinal)
        != TransactionStatusFilterType.ALL.ordinal) count++
    if (sharedPreferences.pull(SharedPreferencesKeys.FILTER_SORT, TransactionSortType.DateDesc.ordinal)
        != TransactionSortType.DateDesc.ordinal) count++
    return count
}