import pandas as pd
import matplotlib.pyplot as plt
import glob
import os

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['SimHei']
plt.rcParams['axes.unicode_minus'] = False

# 读取所有CSV文件
csv_files = glob.glob('results_*.csv')

# 创建一个字典来存储所有数据
all_data = {}
for file in csv_files:
    # 从文件名中提取参数
    params = file.replace('results_', '').replace('.csv', '').split('_')
    replications = int(params[0])
    duration = float(params[1])
    
    # 读取CSV文件
    df = pd.read_csv(file)
    
    # 只保留平均值行
    avg_row = df[df.iloc[:, 0] == 'Average'].iloc[:, 1:]
    
    # 存储数据
    key = (replications, duration)
    all_data[key] = avg_row

# 创建图表目录
if not os.path.exists('charts'):
    os.makedirs('charts')

# 1. 队列长度统计图
def plot_queue_lengths():
    plt.figure(figsize=(12, 6))
    durations = sorted(list(set(k[1] for k in all_data.keys())))
    x = range(len(durations))
    
    for rep in [1, 10, 100, 300]:
        max_a = [all_data[(rep, d)]['Pump A Max Queue'].values[0] for d in durations]
        plt.plot(x, max_a, label=f'重复{rep}次')
    
    plt.title('不同模拟时长下的A泵最大队列长度')
    plt.xlabel('模拟时长（分钟）')
    plt.ylabel('最大队列长度')
    plt.xticks(x, [f'{d/60/24:.0f}天' for d in durations])
    plt.legend()
    plt.grid(True)
    plt.savefig('charts/queue_lengths.png')
    plt.close()

# 2. 等待时间分析图
def plot_wait_times():
    plt.figure(figsize=(12, 6))
    durations = sorted(list(set(k[1] for k in all_data.keys())))
    x = range(len(durations))
    
    for rep in [1, 10, 100, 300]:
        wait_a = [all_data[(rep, d)]['Avg Fuel Wait A'].values[0] for d in durations]
        wait_b = [all_data[(rep, d)]['Avg Fuel Wait B'].values[0] for d in durations]
        plt.plot(x, wait_a, label=f'A泵 ({rep}次)')
        plt.plot(x, wait_b, '--', label=f'B泵 ({rep}次)')
    
    plt.title('不同模拟时长下的平均等待时间')
    plt.xlabel('模拟时长（分钟）')
    plt.ylabel('平均等待时间（分钟）')
    plt.xticks(x, [f'{d/60/24:.0f}天' for d in durations])
    plt.legend()
    plt.grid(True)
    plt.savefig('charts/wait_times.png')
    plt.close()

# 3. 设备利用率对比图
def plot_utilization():
    plt.figure(figsize=(12, 6))
    durations = sorted(list(set(k[1] for k in all_data.keys())))
    x = range(len(durations))
    
    for rep in [100]:  # 使用100次重复的数据作为代表
        util_a = [all_data[(rep, d)]['Pump A Utilization'].values[0] for d in durations]
        util_b = [all_data[(rep, d)]['Pump B Utilization'].values[0] for d in durations]
        util_c = [all_data[(rep, d)]['Clerk Utilization'].values[0] for d in durations]
        
        plt.plot(x, util_a, label='A泵利用率')
        plt.plot(x, util_b, label='B泵利用率')
        plt.plot(x, util_c, label='收银员利用率')
    
    plt.title('设备利用率对比（100次重复）')
    plt.xlabel('模拟时长（分钟）')
    plt.ylabel('利用率（%）')
    plt.xticks(x, [f'{d/60/24:.0f}天' for d in durations])
    plt.legend()
    plt.grid(True)
    plt.savefig('charts/utilization.png')
    plt.close()

# 4. 系统性能随时间变化图
def plot_performance_over_time():
    plt.figure(figsize=(12, 6))
    durations = sorted(list(set(k[1] for k in all_data.keys())))
    x = range(len(durations))
    
    for rep in [1, 10, 100, 300]:
        stay_a = [all_data[(rep, d)]['Avg Stay Time A'].values[0] for d in durations]
        plt.plot(x, stay_a, label=f'重复{rep}次')
    
    plt.title('系统平均逗留时间随时间的变化')
    plt.xlabel('模拟时长（分钟）')
    plt.ylabel('平均逗留时间（分钟）')
    plt.xticks(x, [f'{d/60/24:.0f}天' for d in durations])
    plt.legend()
    plt.grid(True)
    plt.savefig('charts/performance_over_time.png')
    plt.close()

# 5. 员工工作强度对比图
def plot_staff_workload():
    plt.figure(figsize=(12, 6))
    durations = sorted(list(set(k[1] for k in all_data.keys())))
    x = range(len(durations))
    width = 0.25  # 柱状图的宽度
    
    # 只使用重复300次的数据作为最稳定的样本
    rep = 300
    
    # 获取三种员工的工作强度数据
    util_a = [all_data[(rep, d)]['Pump A Utilization'].values[0] for d in durations]
    util_b = [all_data[(rep, d)]['Pump B Utilization'].values[0] for d in durations]
    util_c = [all_data[(rep, d)]['Clerk Utilization'].values[0] for d in durations]
    
    # 绘制柱状图
    plt.bar([i - width for i in x], util_a, width, label='A泵操作员', color='skyblue')
    plt.bar(x, util_b, width, label='B泵操作员', color='lightgreen')
    plt.bar([i + width for i in x], util_c, width, label='收银员', color='salmon')
    
    plt.title('不同岗位员工工作强度对比（300次重复）')
    plt.xlabel('模拟时长')
    plt.ylabel('工作强度（%）')
    plt.xticks(x, [f'{d/60/24:.0f}天' for d in durations])
    plt.legend()
    plt.grid(True)
    plt.savefig('charts/staff_workload.png')
    plt.close()

# 生成所有图表
plot_queue_lengths()
plot_wait_times()
plot_utilization()
plot_performance_over_time()
plot_staff_workload()