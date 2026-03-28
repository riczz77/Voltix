package com.voltix.gamespace

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBoost: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvRamInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.rvGames)
        btnBoost    = findViewById(R.id.btnBoost)
        tvStatus    = findViewById(R.id.tvStatus)
        tvRamInfo   = findViewById(R.id.tvRamInfo)

        updateRamInfo()
        loadGameApps()

        btnBoost.setOnClickListener { boostDevice() }
    }

    private fun boostDevice() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val running = am.runningAppProcesses ?: emptyList()
        var killed = 0
        for (proc in running) {
            if (proc.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                proc.pkgList?.forEach { pkg ->
                    if (pkg != packageName) {
                        try { am.killBackgroundProcesses(pkg); killed++ }
                        catch (_: Exception) {}
                    }
                }
            }
        }
        updateRamInfo()
        tvStatus.text = "✓ Boost berhasil! ($killed proses dibersihkan)"
        tvStatus.setTextColor(getColor(android.R.color.holo_green_light))
    }

    private fun updateRamInfo() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        val avail = mem.availMem / (1024 * 1024)
        val total = mem.totalMem / (1024 * 1024)
        tvRamInfo.text = "RAM: ${avail}MB tersedia / ${total}MB total"
    }

    private fun loadGameApps() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val games = apps.filter { app ->
            val flagGame  = (app.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            val catGame   = if (android.os.Build.VERSION.SDK_INT >= 26)
                                app.category == ApplicationInfo.CATEGORY_GAME else false
            val pkgGame   = listOf("game","games","play","puzzle","arcade","rpg","battle","clash","racing")
                                .any { app.packageName.contains(it, ignoreCase = true) }
            (flagGame || catGame || pkgGame) && pm.getLaunchIntentForPackage(app.packageName) != null
        }.sortedBy { it.loadLabel(pm).toString() }

        tvStatus.text = if (games.isEmpty()) "Tidak ada game ditemukan" else ""

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = GameAdapter(games, pm) { pkg ->
            pm.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
        }
    }
}

// ── Adapter ──────────────────────────────────────────────────────────────────

class GameAdapter(
    private val games: List<ApplicationInfo>,
    private val pm: PackageManager,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<GameAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.ivGameIcon)
        val name: TextView  = v.findViewById(R.id.tvGameName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_game, parent, false))

    override fun getItemCount() = games.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = games[position]
        holder.icon.setImageDrawable(app.loadIcon(pm))
        holder.name.text = app.loadLabel(pm).toString()
        holder.itemView.setOnClickListener { onClick(app.packageName) }
    }
}
