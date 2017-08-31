import numpy as np
import matplotlib as mpl
# from plotly.offline import plot
import plotly.graph_objs as go
# from plotly import tools

mpl.use('Agg')
import matplotlib.pyplot as plt


def plot_thr(traces, path, x_lim=(0, 20), y_lim=(0, 3.5), dim=(8, 6)):
    figure = plt.figure(figsize=dim, dpi=80)
    ax = figure.add_subplot(111)

    # Plot all traces with given parameters
    for (y, x, trace_name, marker, ls) in traces:
        ax.plot(x, y, label=trace_name, marker=marker, linestyle=ls)

    ax.legend(shadow=True, loc='upper right', frameon=False,
              fontsize='medium')
    # ax.set_title('Throughput', fontsize=20, y=1.02)
    ax.set_xlabel('Time (Seconds)', fontsize=12,
                  labelpad=10)
    ax.set_ylabel('max staleness (seconds)', fontsize=12,
                  labelpad=10)
    ax.tick_params(axis='both', which='major', labelsize=12)

    # ax.set_xlim(x_lim)
    # ax.set_ylim(y_lim)
    # ax.grid(True)

    # Save plot with multiple formats
    for ext in ['eps', 'pdf']:
        figure.savefig(path + "out_plot." + ext, bbox_inches='tight')

    plt.show(figure)
    plt.close(figure)

def main():
    data = np.genfromtxt('../../logs/observer.txt', delimiter=' ')

    max_stale = data[0]
    num_stale = data[1]

    ts = len(max_stale)

    # parameters of each trace:
    #         1: y data
    #         2: x data
    #         3: legend name
    #         4: trace marker (',', '>', 's', 'x', 'o', 'd', '<')
    #         5: trace type ['solid', 'dashed', 'dashdot', 'dotted']
    # traces = [
    #     (max_stale, list(range(0, ts)), "precise-newest", "x", "solid"),
    #     (max_stale, list(range(0, ts)), "scuttle-breadth", "x", "solid"),
    #     ...
    # ]

    traces = [
        (max_stale, list(range(0, ts)), "precise-newest", "x", "solid")
    ]
    plot_thr = plot_thr(traces, path="./", x_lim=(0, ts))


if __name__ == '__main__':
    main()