package com.example.trailblazr.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trailblazr.R
import com.example.trailblazr.model.Route

class RouteAdapter(private var routes: List<Route>) : RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    class RouteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val routeName: TextView = view.findViewById(R.id.route_name)
        val routeLength: TextView = view.findViewById(R.id.route_length)
        val bikingTime: TextView = view.findViewById(R.id.biking_time)
        val runningTime: TextView = view.findViewById(R.id.running_time)
        val rating: TextView = view.findViewById(R.id.rating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]
        holder.routeName.text = route.name
        holder.routeLength.text = "${route.lengthInMiles} miles"
        holder.bikingTime.text = "Biking: ${route.bikingTime}"
        holder.runningTime.text = "Running: ${route.runningTime}"
        holder.rating.text = "Rating: ${route.rating} ⭐"
    }

    override fun getItemCount() = routes.size

    fun updateRoutes(newRoutes: List<Route>) {
        routes = newRoutes
        notifyDataSetChanged()
    }
}
