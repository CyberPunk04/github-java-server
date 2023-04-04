package org.fatmansoft.teach.controllers;

import org.fatmansoft.teach.models.*;
import org.fatmansoft.teach.payload.request.DataRequest;
import org.fatmansoft.teach.payload.response.DataResponse;
import org.fatmansoft.teach.util.CommonMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.fatmansoft.teach.repository.*;

import javax.validation.Valid;
import java.util.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/course")
public class CourseController {
    //Java 对象的注入 我们定义的这下Java的操作对象都不能自己管理是由有Spring框架来管理的

    // CourseController中的方法可以直接使用
    @Autowired
    private CourseRepository courseRepository;


    public synchronized Integer getNewCourseId() {  //synchronized 同步方法
        Integer id = courseRepository.getMaxId();  // 查询最大的id
        if (id == null)
            id = 1;
        else
            id = id + 1;
        return id;
    }

    /**
     * getMapFromCourse 将课程表属性数据转换复制MAp集合里
     *
     * @param
     * @return Map
     */

    public Map getMapFromCourse(Course c) {
        Map m = new HashMap();
        if (c == null)
            return m;
        m.put("courseId", c.getCourseId());
        m.put("courseNum", c.getCourseNum());
        m.put("courseName", c.getCourseName());
        m.put("credit", c.getCredit());
        m.put("courseHour", c.getCourseHour());
        m.put("courseType", c.getCourseType());
        m.put("courseDesc", c.getCourseDesc());
        m.put("courseStatus", c.getCourseStatus());
        m.put("courseRemark", c.getCourseRemark());

        return m;
    }

    /**
     * getCourseMapList 根据输入参数查询得到课程数据的 Map List集合 参数为空 查出所有课程， 参数不为空，查出人员编号或人员名称 包含输入字符串的学生
     *
     * @param numName 输入参数
     * @return Map List 集合
     */

    public List getCourseMapList(String numName) {
        List dataList = new ArrayList();
        List<Course> cList = courseRepository.findCourseListByNumName(numName);  //数据库查询操作
        if (cList == null || cList.size() == 0)
            return dataList;
        for (int i = 0; i < cList.size(); i++) {
            dataList.add(getMapFromCourse(cList.get(i)));
        }
        return dataList;
    }

    /**
     * getCourseList 课程管理 点击查询按钮请求
     * 前台请求参数 numName 课程号号或名称的 查询串
     * 返回前端 存储课程信息的 MapList 框架会自动将Map转换程用于前后台传输数据的Json对象，Map的嵌套结构和Json的嵌套结构类似
     *
     * @return
     */


    @PostMapping("/getCourseList")
    @PreAuthorize("hasRole('ADMIN')")//该注解表示只有admin用户才能访问该方法
    public DataResponse getCourseList(@Valid @RequestBody DataRequest dataRequest) {
        String numName = dataRequest.getString("numName");
        List dataList = getCourseMapList(numName);
        return CommonMethod.getReturnData(dataList);  //按照测试框架规范会送Map的list
    }

    /**
     * courseDelete 删除课程信息
     *
     * @param dataRequest 前端courseId 课程编号
     * @return 正常操作
     */


    @PostMapping("/courseDelete")
    @PreAuthorize(" hasRole('ADMIN')")
    public DataResponse courseDelete(@Valid @RequestBody DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        Course c = null;
        Optional<Course> op;
        if (courseId != null) {
            op = courseRepository.findById(courseId);   //查询获得实体对象
            if (op.isPresent()) {
                c = op.get();
            }
        }
        if (c != null) {
            Optional<Course> uOp = courseRepository.findByCourseId(c.getCourseId()); //查询对应该学生的账户
            if (uOp.isPresent()) {
                courseRepository.delete(uOp.get()); //删除对应课程
            }
        }
        return CommonMethod.getReturnMessageOK();  //通知前端操作正常
    }

    /**
     * getCourseInfo 前端点击课程列表时显示详细信息请求服务
     *
     * @param dataRequest 从前端获取 courseId 查询主键
     * @return 根据courseId从数据库中查出数据，存在Map对象里，并返回前端
     */


    @PostMapping("/getCourseInfo")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse getCourseInfo(@Valid @RequestBody DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        Course c = null;
        Optional<Course> op;
        if (courseId != null) {
            op = courseRepository.findById(courseId); //根据学生主键从数据库查询学生的信息
            if (op.isPresent()) {
                c = op.get();
            }
        }
        return CommonMethod.getReturnData(getMapFromCourse(c)); //这里回传包含学生信息的Map对象
    }


    /**
     * courseEditSave 前端学生信息提交服务
     * 前端把所有数据打包成一个Json对象作为参数传回后端，后端直接可以获得对应的Map对象form, 再从form里取出所有属性，
     * 复制到实体对象里，保存到数据库里即可，如果是添加一条记录， id 为空，这是先 new course 计算新的id， 复制相关属性，保存，
     * 如果是编辑原来的信息，courseId不为空。则查询出实体对象，复制相关属性，保存后修改数据库信息，永久修改
     *
     * @return 新建修改课程的主键 course_id 返回前端
     */


    @PostMapping("/courseEditSave")
    @PreAuthorize(" hasRole('ADMIN')")
    public DataResponse courseEditSave(@Valid @RequestBody DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        Map form = dataRequest.getMap("form"); //参数获取Map对象
        String num = CommonMethod.getString(form, "courseNum");  //Map 获取属性的值
        Course c = null;
        Optional<Course> op;
        if (courseId != null) {
            op = courseRepository.findById(courseId);  //查询对应数据库中主键为id的值的实体对象
            if (op.isPresent()) {
                c = op.get();
            }
        }
        Optional<Course> nOp = courseRepository.findByCourseNum(num); //查询是否存在num的人员
        if (nOp.isPresent()) {
            if (c == null || !c.getCourseNum().equals(num)) {
                return CommonMethod.getReturnMessageError("新课程号已经存在，不能添加或修改！");
            }
        }
        if (c == null) {
            courseId = getNewCourseId();//获取Person新的主键，这个是线程同步问题;
            c = new Course();
            c.setCourseId(courseId);
            c.setCourseNum(num);
            courseRepository.saveAndFlush(c);  //插入新的Course记录
        } else {
            courseId = c.getCourseId();
            c.setCourseId(courseId);
        }
        /*if (!num.equals(c.getCourseNum())) {   //如果人员编号变化，修改人员编号和登录账号
            Optional<Course> uOp = courseRepository.findByCourseId(courseId);
            if (uOp.isPresent()) {
                Course u = uOp.get();
                u.setCourseName(num);
                courseRepository.saveAndFlush(u);
            }
            c.setCourseNum(num);  //设置属性
        }*/
        c.setCourseName(CommonMethod.getString(form, "courseName"));
        //c.setCourseName((String)form.get("courseName"));
        c.setCourseHour(CommonMethod.getString(form, "courseHour"));
        c.setCourseType(CommonMethod.getString(form, "courseType"));
        c.setCredit(CommonMethod.getString(form, "credit"));
        c.setCourseStatus(CommonMethod.getString(form, "courseStatus"));
        c.setCourseDesc(CommonMethod.getString(form, "courseDesc"));
        c.setCourseRemark(CommonMethod.getString(form, "courseRemark"));

        courseRepository.save(c);  // 修改保存人员信息
        return CommonMethod.getReturnData(c.getCourseId());
    }


}
