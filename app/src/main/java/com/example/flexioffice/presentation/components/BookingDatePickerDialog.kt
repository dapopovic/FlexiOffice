package com.example.flexioffice.presentation.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.LocalDate

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDatePickerDialog(
        showDatePicker: Boolean,
        selectedDate: LocalDate?,
        onDismiss: () -> Unit,
        onDateSelected: (LocalDate) -> Unit,
) {
    if (showDatePicker) {
        val today = LocalDate.now()
        val datePickerState =
                rememberDatePickerState(
                        initialSelectedDateMillis =
                                selectedDate?.toEpochDay()?.let { it * MILLIS_PER_DAY }
                                        ?: (today.toEpochDay() * MILLIS_PER_DAY),
                        yearRange = today.year..(today.year + 1),
                        selectableDates =
                                object : SelectableDates {
                                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                        val date =
                                                LocalDate.ofEpochDay(
                                                        utcTimeMillis / MILLIS_PER_DAY,
                                                )
                                        return !date.isBefore(today)
                                    }
                                },
                )

        DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val selectedDate =
                                            LocalDate.ofEpochDay(
                                                    millis / MILLIS_PER_DAY,
                                            )
                                    onDateSelected(selectedDate)
                                }
                            },
                    ) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        ) {
            DatePicker(
                    state = datePickerState,
            )
        }
    }
}
