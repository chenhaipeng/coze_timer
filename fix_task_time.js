/**
 * fix_task_time.js
 * 
 * 此脚本用于修复数据库中时间错误的任务。
 * 主要针对年份异常（如2025年）的任务记录进行修复。
 */

const mysql = require('mysql2/promise');
const readline = require('readline');

// 数据库配置
const dbConfig = {
  host: 'localhost',  // 数据库主机地址
  port: 13306,        // 数据库端口
  user: 'root',       // 数据库用户名
  password: 'password', // 数据库密码
  database: 'coze_timer' // 数据库名称
};

// 创建readline接口用于用户交互
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

/**
 * 主函数
 */
async function main() {
  let connection;
  
  try {
    // 连接数据库
    console.log('正在连接数据库...');
    connection = await mysql.createConnection(dbConfig);
    console.log('数据库连接成功！');
    
    // 获取任务表结构
    console.log('检查任务表结构...');
    const [columns] = await connection.execute('SHOW COLUMNS FROM tasks');
    console.log('任务表字段列表:');
    columns.forEach(column => {
      console.log(`- ${column.Field} (${column.Type})`);
    });
    
    // 查询需要修复的任务（年份大于2024的记录）
    console.log('\n查询需要修复的任务记录...');
    const [tasks] = await connection.execute(`
      SELECT task_id, start_time, next_run_time, created_at, updated_at
      FROM tasks
      WHERE YEAR(start_time) > 2024 OR YEAR(next_run_time) > 2024
    `);
    
    // 如果没有需要修复的任务，直接退出
    if (tasks.length === 0) {
      console.log('没有找到需要修复的任务记录！');
      return;
    }
    
    // 显示需要修复的任务
    console.log(`找到 ${tasks.length} 条需要修复的任务记录：`);
    tasks.forEach(task => {
      console.log(`\n任务标识: ${task.task_id}`);
      console.log(`开始时间: ${task.start_time}`);
      console.log(`下次运行时间: ${task.next_run_time}`);
      console.log(`创建时间: ${task.created_at}`);
      console.log(`更新时间: ${task.updated_at}`);
    });
    
    // 提示用户确认是否修复
    await new Promise((resolve) => {
      rl.question('\n请确认是否修复以上任务记录？按 Enter 确认，Ctrl+C 取消: ', () => {
        resolve();
      });
    });
    
    // 修复任务记录
    console.log('\n开始修复任务记录...');
    const [updateResult] = await connection.execute(`
      UPDATE tasks
      SET 
        start_time = DATE_SUB(start_time, INTERVAL 2 YEAR),
        next_run_time = DATE_SUB(next_run_time, INTERVAL 2 YEAR),
        created_at = DATE_SUB(created_at, INTERVAL 2 YEAR),
        updated_at = DATE_SUB(updated_at, INTERVAL 2 YEAR)
      WHERE YEAR(start_time) > 2024 OR YEAR(next_run_time) > 2024
    `);
    
    console.log(`修复完成！已更新 ${updateResult.affectedRows} 条记录。`);
    
    // 验证修复结果
    console.log('\n验证修复结果...');
    const [fixedTasks] = await connection.execute(`
      SELECT task_id, start_time, next_run_time, created_at, updated_at
      FROM tasks
      WHERE task_id IN (${tasks.map(t => `'${t.task_id}'`).join(',')})
    `);
    
    console.log('修复后的任务记录：');
    fixedTasks.forEach(task => {
      console.log(`\n任务标识: ${task.task_id}`);
      console.log(`开始时间: ${task.start_time}`);
      console.log(`下次运行时间: ${task.next_run_time}`);
      console.log(`创建时间: ${task.created_at}`);
      console.log(`更新时间: ${task.updated_at}`);
    });
    
    console.log('\n修复过程成功完成！');
    
  } catch (error) {
    console.error('发生错误:', error);
  } finally {
    // 关闭数据库连接和readline接口
    if (connection) {
      console.log('关闭数据库连接...');
      await connection.end();
    }
    rl.close();
  }
}

// 运行主函数
main().catch(console.error); 