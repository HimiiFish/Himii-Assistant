package com.himii.guga.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.himii.guga.R
import com.himii.guga.ui.theme.GugaTheme
import java.util.concurrent.atomic.AtomicBoolean

class HomeLocationSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GugaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeLocationSettingsScreen(
                        onBackClicked = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, HomeLocationSettingsActivity::class.java)
        }
    }
}

@Composable
private fun HomeLocationSettingsScreen(onBackClicked: () -> Unit) {
    val context = LocalContext.current
    val homeLocationRepository = remember { HomeLocationRepository(context) }
    val divisionCatalog = remember { ChinaAdministrativeDivisionCatalog(context) }

    val countryOptions = remember { divisionCatalog.listCountries() }
    val provinceOptions = remember { divisionCatalog.listProvinces() }

    var selectedCountryName by remember {
        mutableStateOf(countryOptions.first())
    }
    var selectedProvinceNode by remember {
        mutableStateOf<AdministrativeDivisionNode?>(null)
    }
    var selectedCityNode by remember {
        mutableStateOf<AdministrativeDivisionNode?>(null)
    }
    var selectedDistrictNode by remember {
        mutableStateOf<AdministrativeDivisionNode?>(null)
    }

    var statusMessage by remember {
        mutableStateOf(
            homeLocationRepository.loadHomeLocation()?.let { record ->
                formatHomeLocationSummary(record)
            } ?: context.getString(R.string.home_location_not_set)
        )
    }
    var isBusy by remember { mutableStateOf(false) }

    val cityOptions = remember(selectedProvinceNode) {
        selectedProvinceNode?.let { divisionCatalog.listCities(it) }.orEmpty()
    }
    val districtOptions = remember(selectedCityNode) {
        selectedCityNode?.let { divisionCatalog.listDistricts(it) }.orEmpty()
    }

    fun saveCurrentCascadeSelection() {
        val provinceNode = selectedProvinceNode
        val cityNode = selectedCityNode
        val districtNode = selectedDistrictNode
        if (provinceNode == null || cityNode == null || districtNode == null) {
            statusMessage = context.getString(R.string.home_location_cascade_incomplete)
            return
        }
        val homeLocationRecord = divisionCatalog.buildHomeLocationRecord(
            countryName = selectedCountryName,
            provinceNode = provinceNode,
            cityNode = cityNode,
            districtNode = districtNode
        )
        homeLocationRepository.saveHomeLocation(homeLocationRecord)
        statusMessage = formatHomeLocationSummary(homeLocationRecord)
        Toast.makeText(context, R.string.home_location_saved, Toast.LENGTH_SHORT).show()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionResults ->
        val isGranted = permissionResults.values.any { it }
        if (isGranted) {
            refreshWithCurrentDeviceLocation(
                context = context,
                divisionCatalog = divisionCatalog,
                homeLocationRepository = homeLocationRepository,
                onBusyChanged = { isBusy = it },
                onStatusChanged = { statusMessage = it },
                onMatched = { record, provinceNode, cityNode, districtNode ->
                    selectedCountryName = ChinaAdministrativeDivisionCatalog.COUNTRY_NAME_CHINA
                    selectedProvinceNode = provinceNode
                    selectedCityNode = cityNode
                    selectedDistrictNode = districtNode
                    statusMessage = formatHomeLocationSummary(record)
                }
            )
        } else {
            statusMessage = context.getString(R.string.permission_location_denied)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.home_location_settings_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.home_location_settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        CascadeDropdownField(
            label = stringResource(R.string.home_location_cascade_country),
            selectedText = selectedCountryName,
            optionLabels = countryOptions,
            enabled = !isBusy,
            onOptionSelected = { index ->
                selectedCountryName = countryOptions[index]
                selectedProvinceNode = null
                selectedCityNode = null
                selectedDistrictNode = null
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        CascadeDropdownField(
            label = stringResource(R.string.home_location_cascade_province),
            selectedText = selectedProvinceNode?.regionName
                ?: stringResource(R.string.home_location_cascade_please_select),
            optionLabels = provinceOptions.map { it.regionName },
            enabled = !isBusy,
            onOptionSelected = { index ->
                selectedProvinceNode = provinceOptions[index]
                selectedCityNode = null
                selectedDistrictNode = null
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        CascadeDropdownField(
            label = stringResource(R.string.home_location_cascade_city),
            selectedText = selectedCityNode?.regionName
                ?: stringResource(R.string.home_location_cascade_please_select),
            optionLabels = cityOptions.map { it.regionName },
            enabled = !isBusy && selectedProvinceNode != null,
            onOptionSelected = { index ->
                selectedCityNode = cityOptions[index]
                selectedDistrictNode = null
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        CascadeDropdownField(
            label = stringResource(R.string.home_location_cascade_district),
            selectedText = selectedDistrictNode?.regionName
                ?: stringResource(R.string.home_location_cascade_please_select),
            optionLabels = districtOptions.map { it.regionName },
            enabled = !isBusy && selectedCityNode != null,
            onOptionSelected = { index ->
                selectedDistrictNode = districtOptions[index]
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { saveCurrentCascadeSelection() },
            enabled = !isBusy &&
                selectedProvinceNode != null &&
                selectedCityNode != null &&
                selectedDistrictNode != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.home_location_save_selection))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                val fineGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val coarseGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (fineGranted || coarseGranted) {
                    refreshWithCurrentDeviceLocation(
                        context = context,
                        divisionCatalog = divisionCatalog,
                        homeLocationRepository = homeLocationRepository,
                        onBusyChanged = { isBusy = it },
                        onStatusChanged = { statusMessage = it },
                        onMatched = { record, provinceNode, cityNode, districtNode ->
                            selectedCountryName =
                                ChinaAdministrativeDivisionCatalog.COUNTRY_NAME_CHINA
                            selectedProvinceNode = provinceNode
                            selectedCityNode = cityNode
                            selectedDistrictNode = districtNode
                            statusMessage = formatHomeLocationSummary(record)
                        }
                    )
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.home_location_use_current))
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onBackClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun CascadeDropdownField(
    label: String,
    selectedText: String,
    optionLabels: List<String>,
    enabled: Boolean,
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(text = label) },
            modifier = Modifier.fillMaxWidth()
        )
        if (enabled) {
            // matchParentSize 仅在 Box 内容作用域内可用，无需额外 import
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )
        }
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
        ) {
            if (optionLabels.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(R.string.home_location_cascade_empty_options))
                    },
                    onClick = { expanded = false }
                )
            } else {
                optionLabels.forEachIndexed { index, optionLabel ->
                    DropdownMenuItem(
                        text = { Text(text = optionLabel) },
                        onClick = {
                            onOptionSelected(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun refreshWithCurrentDeviceLocation(
    context: Context,
    divisionCatalog: ChinaAdministrativeDivisionCatalog,
    homeLocationRepository: HomeLocationRepository,
    onBusyChanged: (Boolean) -> Unit,
    onStatusChanged: (String) -> Unit,
    onMatched: (
        HomeLocationRecord,
        AdministrativeDivisionNode,
        AdministrativeDivisionNode,
        AdministrativeDivisionNode
    ) -> Unit
) {
    onBusyChanged(true)
    onStatusChanged(context.getString(R.string.home_location_locating))

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val hasHandled = AtomicBoolean(false)
    val mainHandler = Handler(Looper.getMainLooper())

    fun finishWithCoordinates(latitudeDegrees: Double, longitudeDegrees: Double) {
        if (!hasHandled.compareAndSet(false, true)) {
            return
        }
        onStatusChanged(context.getString(R.string.home_location_resolving_place_name))
        Thread {
            val matched = findCascadeMatchForCoordinates(
                divisionCatalog = divisionCatalog,
                latitudeDegrees = latitudeDegrees,
                longitudeDegrees = longitudeDegrees
            )
            mainHandler.post {
                if (matched == null) {
                    onBusyChanged(false)
                    onStatusChanged(context.getString(R.string.home_location_match_failed))
                    return@post
                }
                val (record, provinceNode, cityNode, districtNode) = matched
                homeLocationRepository.saveHomeLocation(record)
                onBusyChanged(false)
                onMatched(record, provinceNode, cityNode, districtNode)
                Toast.makeText(context, R.string.home_location_saved, Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    fun pickBestCachedLocation(): Location? {
        val candidates = mutableListOf<Location>()
        for (providerName in listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )) {
            if (providerName != LocationManager.PASSIVE_PROVIDER &&
                !locationManager.isProviderEnabled(providerName)
            ) {
                continue
            }
            val lastKnown = runCatching {
                locationManager.getLastKnownLocation(providerName)
            }.getOrNull()
            if (lastKnown != null) {
                candidates.add(lastKnown)
            }
        }
        return candidates.minWithOrNull(
            compareBy<Location> { if (it.hasAccuracy()) it.accuracy else Float.MAX_VALUE }
                .thenByDescending { it.time }
        )
    }

    val freshProviders = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    ).filter { locationManager.isProviderEnabled(it) }

    if (freshProviders.isEmpty()) {
        val cached = pickBestCachedLocation()
        if (cached != null) {
            finishWithCoordinates(cached.latitude, cached.longitude)
            return
        }
        onBusyChanged(false)
        onStatusChanged(context.getString(R.string.home_location_provider_disabled))
        return
    }

    val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            freshProviders.forEach {
                runCatching { locationManager.removeUpdates(this) }
            }
            finishWithCoordinates(location.latitude, location.longitude)
        }
    }

    for (providerName in freshProviders) {
        runCatching {
            locationManager.requestLocationUpdates(
                providerName,
                0L,
                0f,
                locationListener,
                Looper.getMainLooper()
            )
        }
    }

    mainHandler.postDelayed({
        if (hasHandled.get()) {
            return@postDelayed
        }
        freshProviders.forEach {
            runCatching { locationManager.removeUpdates(locationListener) }
        }
        val cached = pickBestCachedLocation()
        if (cached != null) {
            finishWithCoordinates(cached.latitude, cached.longitude)
        } else {
            onBusyChanged(false)
            onStatusChanged(context.getString(R.string.home_location_locate_timeout))
        }
    }, LOCATION_FIX_TIMEOUT_MILLIS)
}

private fun findCascadeMatchForCoordinates(
    divisionCatalog: ChinaAdministrativeDivisionCatalog,
    latitudeDegrees: Double,
    longitudeDegrees: Double
): Quadruple<
    HomeLocationRecord,
    AdministrativeDivisionNode,
    AdministrativeDivisionNode,
    AdministrativeDivisionNode
    >? {
    var bestDistanceMeters = Double.MAX_VALUE
    var best: Quadruple<
        HomeLocationRecord,
        AdministrativeDivisionNode,
        AdministrativeDivisionNode,
        AdministrativeDivisionNode
        >? = null

    for (provinceNode in divisionCatalog.listProvinces()) {
        for (cityNode in divisionCatalog.listCities(provinceNode)) {
            for (districtNode in divisionCatalog.listDistricts(cityNode)) {
                val districtLatitude = districtNode.latitudeDegrees ?: continue
                val districtLongitude = districtNode.longitudeDegrees ?: continue
                val distanceMeters = approximateDistanceMeters(
                    latitudeDegrees,
                    longitudeDegrees,
                    districtLatitude,
                    districtLongitude
                )
                if (distanceMeters < bestDistanceMeters) {
                    bestDistanceMeters = distanceMeters
                    val record = divisionCatalog.buildHomeLocationRecord(
                        countryName = ChinaAdministrativeDivisionCatalog.COUNTRY_NAME_CHINA,
                        provinceNode = provinceNode,
                        cityNode = cityNode,
                        districtNode = districtNode
                    )
                    best = Quadruple(record, provinceNode, cityNode, districtNode)
                }
            }
        }
    }
    if (best == null || bestDistanceMeters > 80_000.0) {
        return null
    }
    return best
}

private fun approximateDistanceMeters(
    latitudeOne: Double,
    longitudeOne: Double,
    latitudeTwo: Double,
    longitudeTwo: Double
): Double {
    val latitudeDelta = latitudeOne - latitudeTwo
    val longitudeDelta = longitudeOne - longitudeTwo
    // 粗略平面近似，仅用于就近匹配区县中心
    val metersPerDegree = 111_000.0
    return Math.sqrt(
        latitudeDelta * latitudeDelta + longitudeDelta * longitudeDelta
    ) * metersPerDegree
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

private fun formatHomeLocationSummary(homeLocationRecord: HomeLocationRecord): String {
    return "常住地：${homeLocationRecord.displayName}"
}

private const val LOCATION_FIX_TIMEOUT_MILLIS = 12_000L
