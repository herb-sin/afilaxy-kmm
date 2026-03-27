package com.afilaxy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afilaxy.domain.model.*
import com.afilaxy.presentation.professional.ProfessionalDashboardState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalDashboardScreen(
    state: ProfessionalDashboardState,
    onPeriodSelected: (String) -> Unit,
    onAlertClick: (String) -> Unit,
    onPatientClick: (String) -> Unit,
    onEmergencyCenter: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Header
        item {
            ProfessionalHeader()
        }

        // Metrics Dashboard
        state.dashboard?.let { dashboard ->
            item {
                MetricsDashboard(dashboard = dashboard)
            }
        }

        // Crisis Frequency Chart
        item {
            CrisisFrequencyCard(
                crisisData = state.crisisAnalytics,
                selectedPeriod = state.selectedPeriod,
                onPeriodSelected = onPeriodSelected
            )
        }

        // Recent Alerts
        item {
            RecentAlertsCard(
                alerts = state.alerts,
                onAlertClick = onAlertClick
            )
        }

        // Emergency Center Button
        item {
            Button(
                onClick = onEmergencyCenter,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Emergency,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ABRIR CENTRAL DE EMERGÊNCIA",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Priority Patients List
        item {
            PriorityPatientsCard(
                patients = state.patients.filter { 
                    it.status == PatientStatus.URGENTE || it.status == PatientStatus.ALERTA 
                },
                onPatientClick = onPatientClick
            )
        }
    }
}

@Composable
private fun ProfessionalHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Área do Profissional",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Monitoramento em tempo real e análise preditiva de pacientes asmáticos sob seus cuidados.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun MetricsDashboard(dashboard: ProfessionalDashboard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "TOTAL DE PACIENTES",
                    value = "${dashboard.totalPatients}",
                    subtitle = "+12 este mês",
                    color = Color(0xFF1976D2)
                )
                
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "ALERTAS CRÍTICOS",
                    value = "${dashboard.criticalAlerts}",
                    subtitle = "Ação imediata necessária",
                    color = Color(0xFFD32F2F)
                )
            }
            
            // Bottom Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "ADESÃO MÉDIA",
                    value = "${(dashboard.adherenceRate * 100).toInt()}%",
                    subtitle = "Dentro da meta",
                    color = Color(0xFF4CAF50)
                )
                
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "TELECONSULTAS",
                    value = "${dashboard.upcomingConsultations.size}",
                    subtitle = "Próxima às 14h05",
                    color = Color(0xFF1976D2)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
            
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CrisisFrequencyCard(
    crisisData: List<CrisisData>,
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Frequência de Crises",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Visão consolidada dos últimos 30 dias por gravidade",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { onPeriodSelected("MÊS") },
                        label = { Text("MÊS") },
                        selected = selectedPeriod == "MÊS"
                    )
                    FilterChip(
                        onClick = { onPeriodSelected("SEMANA") },
                        label = { Text("SEMANA") },
                        selected = selectedPeriod == "SEMANA"
                    )
                }
            }
            
            // Simple bar chart representation
            CrisisChart(crisisData = crisisData)
        }
    }
}

@Composable
private fun CrisisChart(crisisData: List<CrisisData>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        crisisData.take(10).forEach { data ->
            val maxCount = crisisData.maxOfOrNull { it.count } ?: 1
            val height = ((data.count.toFloat() / maxCount) * 100).dp
            
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(
                            when (data.severity) {
                                CrisisSeverity.LEVE -> Color(0xFF4CAF50)
                                CrisisSeverity.MODERADA -> Color(0xFFFF9800)
                                CrisisSeverity.GRAVE -> Color(0xFFFF5722)
                                CrisisSeverity.MUITO_GRAVE -> Color(0xFFD32F2F)
                            }
                        )
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = data.date.takeLast(2),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun RecentAlertsCard(
    alerts: List<PatientAlert>,
    onAlertClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Alertas Recentes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            alerts.take(3).forEach { alert ->
                AlertItem(
                    alert = alert,
                    onClick = { onAlertClick(alert.id) }
                )
            }
        }
    }
}

@Composable
private fun AlertItem(
    alert: PatientAlert,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isUrgent) 
                Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(
                        if (alert.isUrgent) Color(0xFFD32F2F) else Color(0xFFFF9800),
                        RoundedCornerShape(2.dp)
                    )
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.patientName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Surface(
                        color = if (alert.isUrgent) Color(0xFFD32F2F) else Color(0xFFFF9800),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (alert.isUrgent) "URGENTE" else "ALERTA",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Text(
                    text = alert.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                TextButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "VER PRONTUÁRIO >",
                        fontSize = 10.sp,
                        color = Color(0xFF1976D2)
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityPatientsCard(
    patients: List<PatientSummary>,
    onPatientClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Lista de Pacientes Prioritários",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PACIENTE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "STATUS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "ÚLT. CRISE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            patients.forEach { patient ->
                PatientItem(
                    patient = patient,
                    onClick = { onPatientClick(patient.id) }
                )
            }
        }
    }
}

@Composable
private fun PatientItem(
    patient: PatientSummary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1976D2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = patient.initials,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column {
                    Text(
                        text = patient.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when (patient.asmaType) {
                            AsmaType.INTERMITENTE -> "Intermitente"
                            AsmaType.PERSISTENTE_LEVE -> "Persistente Leve"
                            AsmaType.PERSISTENTE_MODERADA -> "Persistente Moderada"
                            AsmaType.PERSISTENTE_GRAVE -> "Persistente Grave"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Surface(
                color = when (patient.status) {
                    PatientStatus.CONTROLADO -> Color(0xFF4CAF50)
                    PatientStatus.ALERTA -> Color(0xFFFF9800)
                    PatientStatus.ESTAVEL -> Color(0xFF2196F3)
                    PatientStatus.URGENTE -> Color(0xFFD32F2F)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = when (patient.status) {
                        PatientStatus.CONTROLADO -> "CONTROLADO"
                        PatientStatus.ALERTA -> "ALERTA"
                        PatientStatus.ESTAVEL -> "ESTÁVEL"
                        PatientStatus.URGENTE -> "URGENTE"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Text(
                text = patient.lastCrisis,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}