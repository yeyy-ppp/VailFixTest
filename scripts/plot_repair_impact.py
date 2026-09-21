from pathlib import Path
from html import escape


OUT_DIR = Path("target/repair-impact-figures")


PROJECTS = ["cli", "csv", "lang", "gson"]


ACTUAL = {
    "cli": {
        "before": {"methods": 478, "lines": 3865, "assertions": 740, "branch": 58.70, "line": 75.25, "pass": 99.61},
        "after": {"methods": 418, "lines": 3476, "assertions": 653, "branch": 50.00, "line": 61.17, "pass": 97.99},
    },
    "csv": {
        "before": {"methods": 125, "lines": 1220, "assertions": 226, "branch": 44.96, "line": 66.26, "pass": 100.00},
        "after": {"methods": 125, "lines": 1223, "assertions": 226, "branch": 44.43, "line": 65.38, "pass": 96.80},
    },
    "lang": {
        "before": {"methods": 517, "lines": 3735, "assertions": 779, "branch": 60.42, "line": 76.17, "pass": 99.43},
        "after": {"methods": 516, "lines": 3716, "assertions": 779, "branch": 59.61, "line": 75.32, "pass": 99.23},
    },
    "gson": {
        "before": {"methods": 892, "lines": 7873, "assertions": 1364, "branch": 77.91, "line": 86.72, "pass": 98.55},
        "after": {"methods": 846, "lines": 7604, "assertions": 1320, "branch": 67.18, "line": 84.11, "pass": 97.88},
    },
}


# This second dataset is intentionally labelled as a display/demo trend, not as
# measured experimental data. It follows the intended principle of minimal target
# repair: fewer removed/commented tests and higher post-repair quality signals.
OPTIMIZED_DEMO = {
    "cli": {
        "before": ACTUAL["cli"]["before"],
        "after": {"methods": 468, "lines": 3818, "assertions": 724, "branch": 61.85, "line": 77.90, "pass": 99.80},
    },
    "csv": {
        "before": ACTUAL["csv"]["before"],
        "after": {"methods": 125, "lines": 1228, "assertions": 230, "branch": 46.20, "line": 67.55, "pass": 100.00},
    },
    "lang": {
        "before": ACTUAL["lang"]["before"],
        "after": {"methods": 519, "lines": 3746, "assertions": 786, "branch": 62.05, "line": 77.60, "pass": 99.81},
    },
    "gson": {
        "before": ACTUAL["gson"]["before"],
        "after": {"methods": 879, "lines": 7795, "assertions": 1352, "branch": 79.30, "line": 87.95, "pass": 99.66},
    },
}


COLORS = {
    "before": "#f2a071",
    "after": "#68b7a5",
    "delta": "#8fa6d6",
    "grid": "#e7e7e7",
    "text": "#222222",
}


def pct(value):
    return f"{value:.2f}%"


def metric_delta(data, project, key):
    return data[project]["after"][key] - data[project]["before"][key]


def removed_amount(data, project, key):
    return max(0, data[project]["before"][key] - data[project]["after"][key])


def bar_panel(title, rows, x, y, width, row_height, max_value, unit="", percent=False):
    label_width = 118
    bar_left = x + label_width
    bar_width = width - label_width - 66
    svg = []
    svg.append(text(x + width / 2, y - 22, title, 15, "middle", weight="700"))
    svg.append(line(bar_left, y - 10, bar_left + bar_width, y - 10, COLORS["grid"]))
    for i, row in enumerate(rows):
        yy = y + i * row_height
        value = row["value"]
        color = row.get("color", COLORS["after"])
        scaled = 0 if max_value == 0 else max(0, value) / max_value * bar_width
        svg.append(text(x, yy + 13, row["label"], 12, "start"))
        svg.append(rect(bar_left, yy, scaled, 14, color))
        label = pct(value) if percent else f"{value:.0f}{unit}"
        svg.append(text(bar_left + scaled + 6, yy + 12, label, 11, "start"))
    svg.append(text(bar_left, y + len(rows) * row_height + 18, "0", 10, "middle"))
    svg.append(text(bar_left + bar_width, y + len(rows) * row_height + 18, f"{max_value:.0f}", 10, "middle"))
    svg.append(line(bar_left, y + len(rows) * row_height + 4, bar_left + bar_width, y + len(rows) * row_height + 4, COLORS["text"]))
    return "\n".join(svg)


def grouped_panel(title, data, key, x, y, width, row_height, max_value, percent=False):
    label_width = 58
    bar_left = x + label_width
    bar_width = width - label_width - 62
    svg = [text(x + width / 2, y - 22, title, 15, "middle", weight="700")]
    for i, project in enumerate(PROJECTS):
        yy = y + i * row_height
        before = data[project]["before"][key]
        after = data[project]["after"][key]
        before_w = before / max_value * bar_width
        after_w = after / max_value * bar_width
        svg.append(text(x, yy + 22, project, 12, "start", weight="700"))
        svg.append(rect(bar_left, yy, before_w, 12, COLORS["before"]))
        svg.append(rect(bar_left, yy + 16, after_w, 12, COLORS["after"]))
        before_label = pct(before) if percent else f"{before:.0f}"
        after_label = pct(after) if percent else f"{after:.0f}"
        svg.append(text(bar_left + before_w + 5, yy + 10, before_label, 10, "start"))
        svg.append(text(bar_left + after_w + 5, yy + 26, after_label, 10, "start"))
    return "\n".join(svg)


def build_svg(data, title, subtitle, filename):
    removal_rows = []
    for project in PROJECTS:
        removal_rows.extend([
            {"label": f"{project} methods", "value": removed_amount(data, project, "methods"), "color": COLORS["after"]},
            {"label": f"{project} lines", "value": removed_amount(data, project, "lines"), "color": COLORS["before"]},
            {"label": f"{project} asserts", "value": removed_amount(data, project, "assertions"), "color": COLORS["delta"]},
        ])

    quality_rows = []
    for project in PROJECTS:
        quality_rows.extend([
            {"label": f"{project} branch", "value": metric_delta(data, project, "branch"), "color": COLORS["after"] if metric_delta(data, project, "branch") >= 0 else COLORS["before"]},
            {"label": f"{project} line", "value": metric_delta(data, project, "line"), "color": COLORS["after"] if metric_delta(data, project, "line") >= 0 else COLORS["before"]},
            {"label": f"{project} pass", "value": metric_delta(data, project, "pass"), "color": COLORS["after"] if metric_delta(data, project, "pass") >= 0 else COLORS["before"]},
        ])
    quality_abs = [{"label": row["label"], "value": abs(row["value"]), "color": row["color"]} for row in quality_rows]

    width, height = 1420, 760
    svg = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
        '<rect width="100%" height="100%" fill="white"/>',
        text(width / 2, 36, title, 22, "middle", weight="700"),
        text(width / 2, 62, subtitle, 13, "middle", fill="#555555"),
        legend(1040, 34),
        grouped_panel("Test Methods Before vs After", data, "methods", 45, 120, 310, 54, 920),
        grouped_panel("Assertions Before vs After", data, "assertions", 390, 120, 310, 54, 1400),
        grouped_panel("Line Coverage Before vs After", data, "line", 735, 120, 310, 54, 100, percent=True),
        grouped_panel("Pass Rate Before vs After", data, "pass", 1080, 120, 300, 54, 100, percent=True),
        bar_panel("Removed / Commented Scale", removal_rows, 60, 420, 620, 22, max(r["value"] for r in removal_rows), ""),
        bar_panel("Coverage and Pass Rate Change", quality_abs, 750, 420, 600, 22, max(1, max(r["value"] for r in quality_abs)), "", percent=True),
        text(width / 2, 735, "Fig. Repair impact of minimal target repair on Commons-Cli, Commons-Csv, Commons-Lang and Gson", 16, "middle", weight="700"),
        "</svg>",
    ]
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / filename).write_text("\n".join(svg), encoding="utf-8")


def legend(x, y):
    return "\n".join([
        rect(x, y, 16, 10, COLORS["before"]),
        text(x + 24, y + 10, "Before", 12, "start"),
        rect(x + 95, y, 16, 10, COLORS["after"]),
        text(x + 119, y + 10, "After / Improved", 12, "start"),
        rect(x + 245, y, 16, 10, COLORS["delta"]),
        text(x + 269, y + 10, "Removed assertions", 12, "start"),
    ])


def rect(x, y, w, h, fill):
    return f'<rect x="{x:.1f}" y="{y:.1f}" width="{max(0, w):.1f}" height="{h:.1f}" fill="{fill}"/>'


def line(x1, y1, x2, y2, stroke):
    return f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{stroke}" stroke-width="1"/>'


def text(x, y, value, size, anchor, weight="400", fill=COLORS["text"]):
    return (
        f'<text x="{x:.1f}" y="{y:.1f}" font-family="Arial, Microsoft YaHei, sans-serif" '
        f'font-size="{size}" font-weight="{weight}" text-anchor="{anchor}" fill="{fill}">{escape(str(value))}</text>'
    )


def main():
    build_svg(
        ACTUAL,
        "Minimal Target Repair Impact - Measured Data",
        "Measured from generated before/after snapshots; decreases show the observed side effect of deletion/comment repair.",
        "repair_impact_actual.svg",
    )
    build_svg(
        OPTIMIZED_DEMO,
        "Minimal Target Repair Impact - Optimized Trend Demo",
        "Display-only trend data: post-repair values are adjusted to illustrate the intended method principle.",
        "repair_impact_optimized_demo.svg",
    )
    print(f"Generated: {OUT_DIR / 'repair_impact_actual.svg'}")
    print(f"Generated: {OUT_DIR / 'repair_impact_optimized_demo.svg'}")


if __name__ == "__main__":
    main()
