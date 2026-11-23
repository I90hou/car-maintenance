package com.carmaintenance.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carmaintenance.app.ui.theme.AppText
import com.carmaintenance.app.ui.theme.Cockpit

/**
 * 发丝下划线输入行
 *
 * 本设计的表单不用圆角输入框：只有一行小字标签、一条发丝线和输入内容。
 */
@Composable
fun UnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    textStyle: TextStyle = AppText.RowMeta.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 21.sp
    ),
    isError: Boolean = false,
    errorText: String? = null,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(label)
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (readOnly && onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            if (readOnly) {
                Text(
                    value.ifBlank { placeholder.orEmpty() },
                    style = textStyle,
                    color = if (value.isBlank()) Cockpit.Text3 else Cockpit.Text
                )
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            placeholder,
                            style = textStyle.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            ),
                            color = Cockpit.Text3
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = textStyle.copy(color = Cockpit.Text),
                        cursorBrush = SolidColor(Cockpit.Accent),
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(10.dp))
        HairDivider(color = if (isError) Cockpit.Danger else Cockpit.Line)
        if (isError && errorText != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                errorText,
                style = AppText.Hint,
                color = Cockpit.Danger
            )
        }
    }
}

/**
 * 多行发丝下划线输入（备注）
 */
@Composable
fun UnderlineTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minLines: Int = 3
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(label)
        Spacer(Modifier.height(8.dp))
        Box {
            if (value.isEmpty() && placeholder != null) {
                Text(
                    placeholder,
                    style = AppText.RowMeta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                    color = Cockpit.Text3
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = AppText.RowMeta.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = Cockpit.Text
                ),
                cursorBrush = SolidColor(Cockpit.Accent),
                minLines = minLines,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(10.dp))
        HairDivider()
    }
}

/**
 * 二选一的胶囊分段控件
 */
@Composable
fun CockpitSegment(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            androidx.compose.material3.Surface(
                onClick = { onSelect(index) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                color = if (selected) Cockpit.Accent else Cockpit.Surface2,
                contentColor = if (selected) Cockpit.OnAccent else Cockpit.Text2,
                border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Cockpit.Line),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 11.dp)
                ) {
                    Text(
                        label,
                        style = AppText.RowMeta.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 12.5.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
