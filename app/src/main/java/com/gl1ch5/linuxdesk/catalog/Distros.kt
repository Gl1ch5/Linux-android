package com.gl1ch5.linuxdesk.catalog

/** How a distro is shown on screen. Mirrors `session` in the planned images.json. */
enum class SessionType { X11, WaylandNested }

data class Distro(
    val id: String,
    val name: String,
    val description: String,
    val session: SessionType,
)

/**
 * Static catalog for stage 1. Stage 2 replaces it with images.json from the Releases page.
 * Only the lightweight entry is meant to work in stage 1; the others are placeholders.
 */
object Distros {
    val all = listOf(
        Distro(
            id = "xfce-light",
            name = "XFCE (лёгкий)",
            description = "Debian minimal + XFCE. Цель: до 500 МБ распакованным.",
            session = SessionType.X11,
        ),
        Distro(
            id = "omarchy",
            name = "Omarchy",
            description = "Arch Linux ARM + Hyprland через вложенный Weston. Этап 3.",
            session = SessionType.WaylandNested,
        ),
        Distro(
            id = "macstyle",
            name = "Десктоп в стиле macOS",
            description = "KDE Plasma или XFCE + тема, док и глобальное меню. Этап 3.",
            session = SessionType.WaylandNested,
        ),
        Distro(
            id = "gnome",
            name = "GNOME",
            description = "GNOME под вложенным Weston. Этап 4, после исследования.",
            session = SessionType.WaylandNested,
        ),
    )
}
