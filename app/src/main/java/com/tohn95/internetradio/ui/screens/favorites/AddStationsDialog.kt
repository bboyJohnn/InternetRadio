package com.tohn95.internetradio.ui.screens.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tohn95.internetradio.R
import com.tohn95.internetradio.domain.model.Station

@Composable
fun AddStationsDialog(
    favorites: List<Station>,
    initiallySelected: Set<String>,
    onDismiss: () -> Unit,
    onDone: (Set<String>) -> Unit,
) {
    var selected by remember { mutableStateOf(initiallySelected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_add_stations)) },
        text = {
            Column {
                if (favorites.isEmpty()) Text(stringResource(R.string.empty_favorites))
                LazyColumn(Modifier.heightIn(max = 440.dp)) {
                    items(favorites, key = { it.uuid }) { station ->
                        val checked = station.uuid in selected
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                selected = if (checked) selected - station.uuid else selected + station.uuid
                            }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = {
                                selected = if (it) selected + station.uuid else selected - station.uuid
                            })
                            Text(station.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(selected) }) { Text(stringResource(R.string.dialog_done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}
