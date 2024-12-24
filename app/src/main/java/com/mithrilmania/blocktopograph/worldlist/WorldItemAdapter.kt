package com.mithrilmania.blocktopograph.worldlist

import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.CardWorldItemBinding
import com.mithrilmania.blocktopograph.world.WorldInfo
import java.util.*

class WorldItemAdapter(
    val model: WorldListModel
) : RecyclerView.Adapter<WorldItemAdapter.WorldItemHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorldItemHolder {
        val context = parent.context
        return WorldItemHolder(
            context,
            this.model,
            CardWorldItemBinding.inflate(LayoutInflater.from(context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: WorldItemHolder, position: Int) {
        val world = this.model.worlds[position]
        val binding = holder.binding
        holder.world = world
        binding.mode.text = world.mode
        binding.root.setOnClickListener(holder)
        if (world.icon == null) {
            binding.icon.setImageResource(R.drawable.world_icon_default)
        } else {
            binding.icon.setImageBitmap(world.icon)
        }
        binding.name.text = world.name
        binding.date.text = DateFormat.getDateFormat(holder.context).format(Date(world.time))
        binding.size.text = world.size ?: holder.context.getString(R.string.calculating_size)
        binding.path.text = world.path
        binding.behavior.text = world.behavior.toString()
        binding.resource.text = world.resource.toString()
    }

    fun addWorld(world: WorldInfo<*>): Boolean {
        val worlds = this.model.worlds
        synchronized(worlds) {
            var flag = true
            var index = worlds.size
            while (--index >= 0) {
                if (world.time < worlds[index].time) {
                    if (++index != worlds.size && world == worlds[index]) return false
                    worlds.add(index, world)
                    flag = false
                    break
                }
            }
            if (flag) {
                if (worlds.firstOrNull() == world) return false
                worlds.add(0, world)
            }
            this.notifyItemInserted(index)
            return true
        }
    }

    fun notifyItemChanged(world: WorldInfo<*>) {
        val worlds = this.model.worlds
        synchronized(worlds) {
            val index = worlds.indexOf(world)
            if (index == -1) return
            this.notifyItemChanged(index)
            this.model.selected.let {
                if (world == it.value) {
                    it.value = world // notify
                }
            }
        }
    }

    class WorldItemHolder(
        val context: Context,
        val model: WorldListModel,
        val binding: CardWorldItemBinding
    ) : RecyclerView.ViewHolder(binding.root), OnClickListener {
        var world: WorldInfo<*>? = null
        override fun onClick(v: View?) {
            this.model.selected.value = this.world ?: return
        }
    }

    override fun getItemCount(): Int = this.model.worlds.size
}