/**
 * 系统时间和时区问题修复脚本
 */

const http = require('http');
const mysql = require('mysql2/promise');

// 数据库连接配置
const dbConfig = {
  host: 'localhost',
  port: 13306,
  user: 'root',
  password: 'password',
  database: 'coze_timer'
};

// 主函数
async function main() {
  console.log("======= 系统时间与时区问题诊断 =======");
  
  try {
    // 检查系统时间
    const systemTime = new Date();
    console.log("本地系统时间:", systemTime.toString());
    console.log("本地时间ISO:", systemTime.toISOString());
    console.log("本地时间时区偏移:", systemTime.getTimezoneOffset() / -60, "小时");
    
    // 检查数据库连接和时间
    await checkDatabaseTime();
    
    // 检查容器时间
    await checkContainerTime();
    
    // 检查数据库中的任务时间格式
    await checkTaskTimesInDatabase();
    
    console.log("\n======= 时间问题解决方案 =======");
    console.log("1. 修复系统年份问题:");
    console.log("   - 在宿主机上运行: sudo date -u MMDDHHmmYYYY");
    console.log("   - 例如设置为2023年4月22日: sudo date -u 042216002023");
    console.log("   - 然后重启docker: docker compose down && docker compose up -d");
    
    console.log("\n2. 修复数据库中现有任务的时间:");
    console.log("   - 执行以下SQL来修复现有任务的时间：");
    console.log("   UPDATE tasks SET ");
    console.log("     start_time = DATE_SUB(start_time, INTERVAL 2 YEAR),");
    console.log("     next_run_time = DATE_SUB(next_run_time, INTERVAL 2 YEAR),");
    console.log("     created_at = DATE_SUB(created_at, INTERVAL 2 YEAR),");
    console.log("     updated_at = DATE_SUB(updated_at, INTERVAL 2 YEAR)");
    console.log("   WHERE YEAR(start_time) > 2024;");
    
  } catch (error) {
    console.error("执行过程中发生错误:", error);
  }
}

// 检查数据库连接和时间
async function checkDatabaseTime() {
  console.log("\n----- 检查数据库时间 -----");
  try {
    const connection = await mysql.createConnection(dbConfig);
    
    // 检查数据库时间
    const [rows] = await connection.execute('SELECT NOW() AS db_time, @@system_time_zone AS db_timezone, @@global.time_zone AS global_timezone, @@session.time_zone AS session_timezone');
    if (rows.length > 0) {
      console.log("数据库时间:", rows[0].db_time.toString());
      console.log("数据库系统时区:", rows[0].db_timezone);
      console.log("数据库全局时区:", rows[0].global_timezone);
      console.log("数据库会话时区:", rows[0].session_timezone);
    }
    
    await connection.end();
  } catch (error) {
    console.error("检查数据库时间时出错:", error);
  }
}

// 检查容器时间
async function checkContainerTime() {
  console.log("\n----- 检查容器时间 -----");
  return new Promise((resolve) => {
    const options = {
      hostname: 'localhost',
      port: 8080,
      path: '/api/v1/system/time',
      method: 'GET'
    };
    
    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        try {
          if (res.statusCode !== 200) {
            console.log("无法获取容器时间API响应，状态码:", res.statusCode);
            // 尝试连接容器内时间
            console.log("请手动执行: docker exec coze_timer_app date");
            resolve();
            return;
          }
          
          const parsedData = JSON.parse(responseData);
          if (parsedData.currentTime) {
            console.log("容器时间API响应:", parsedData.currentTime);
          } else {
            console.log("容器时间API响应格式不符合预期:", responseData);
          }
          resolve();
        } catch (e) {
          console.error("解析容器时间响应失败:", e);
          resolve();
        }
      });
    });
    
    req.on('error', (e) => {
      console.error(`获取容器时间错误: ${e.message}`);
      console.log("请手动执行: docker exec coze_timer_app date");
      resolve();
    });
    
    req.end();
  });
}

// 检查数据库中的任务时间格式
async function checkTaskTimesInDatabase() {
  console.log("\n----- 检查数据库中的任务时间 -----");
  try {
    const connection = await mysql.createConnection(dbConfig);
    
    // 获取最新的5条任务
    const [rows] = await connection.execute('SELECT task_id, start_time, next_run_time, created_at, updated_at FROM tasks ORDER BY created_at DESC LIMIT 5');
    
    if (rows.length > 0) {
      console.log(`找到 ${rows.length} 条任务记录：`);
      rows.forEach((task, index) => {
        console.log(`\n任务 ${index + 1}:`);
        console.log("任务ID:", task.task_id);
        console.log("开始时间:", task.start_time ? task.start_time.toString() : null);
        console.log("下次执行时间:", task.next_run_time ? task.next_run_time.toString() : null);
        console.log("创建时间:", task.created_at ? task.created_at.toString() : null);
        console.log("更新时间:", task.updated_at ? task.updated_at.toString() : null);
        
        // 检查年份
        if (task.start_time && task.start_time.getFullYear() > 2024) {
          console.log("警告: 开始时间年份异常 -", task.start_time.getFullYear());
        }
      });
    } else {
      console.log("数据库中没有找到任务记录");
    }
    
    await connection.end();
  } catch (error) {
    console.error("检查任务时间时出错:", error);
  }
}

// 执行主函数
main(); 