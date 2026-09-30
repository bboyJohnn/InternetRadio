package com.tohn95.internetradio.ui.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tohn95.internetradio.R
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.ui.components.onPrimary
import com.tohn95.internetradio.ui.theme.LocalPalette
import java.util.Locale
import java.util.UUID

/**
 * «Своя станция» по ссылке (идея radioMii): нужен только адрес потока. «Проверить» сразу включает поток,
 * «Добавить» — в Избранное. uuid с префиксом custom- — в каталог radio-browser клики/голоса не шлём.
 */
@Composable
fun CustomStationDialog(onDismiss: () -> Unit, onTest: (Station) -> Unit, onAdd: (Station) -> Unit) {
    val palette = LocalPalette.current
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var homepage by rememberSaveable { mutableStateOf("") }
    var logo by rememberSaveable { mutableStateOf("") }
    var uuid by rememberSaveable { mutableStateOf(Station.CUSTOM_PREFIX + UUID.randomUUID().toString()) }

    val urlOk = url.trim().let { it.startsWith("http://", true) || it.startsWith("https://", true) }
    val canSave = name.isNotBlank() && urlOk

    fun build(): Station {
        val code = country.trim().uppercase().take(2)
        val u = url.trim()
        return Station(
            uuid = uuid, name = name.trim(), streamUrl = u,
            faviconUrl = logo.trim().takeIf { it.startsWith("http", true) },
            tags = tags.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() },
            country = if (code.length == 2) Locale("", code).getDisplayCountry(Locale.getDefault()) else "",
            countryCode = if (code.length == 2) code else "",
            bitrate = 0, codec = "", votes = 0, clickCount = 0,
            isHls = u.contains(".m3u8", ignoreCase = true),
            homepage = homepage.trim().takeIf { it.startsWith("http", true) },
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(palette.cardBg)
                .verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(palette.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Link, null, tint = palette.primary) }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(stringResource(R.string.custom_title), color = palette.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.custom_hint), color = palette.textMuted, fontSize = 12.sp, lineHeight = 15.sp)
                }
            }
            Field(name, { name = it }, stringResource(R.string.custom_name))
            Field(
                url, { url = it }, stringResource(R.string.custom_url), KeyboardType.Uri,
                error = url.isNotBlank() && !urlOk,
            )
            if (url.isNotBlank() && !urlOk) {
                Text(stringResource(R.string.custom_url_invalid), color = palette.red, fontSize = 12.sp)
            }
            Field(tags, { tags = it }, stringResource(R.string.custom_tags))
            Field(country, { country = it.take(2) }, stringResource(R.string.custom_country))
            Field(homepage, { homepage = it }, stringResource(R.string.custom_homepage), KeyboardType.Uri)
            Field(logo, { logo = it }, stringResource(R.string.custom_logo), KeyboardType.Uri)

            OutlinedButton(
                onClick = { onTest(build()) }, enabled = canSave,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.custom_test), modifier = Modifier.padding(start = 4.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
                Button(
                    onClick = { onAdd(build()); uuid = Station.CUSTOM_PREFIX + UUID.randomUUID().toString() },
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary()),
                ) { Text(stringResource(R.string.fav_add)) }
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    error: Boolean = false,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, isError = error,
        shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}
